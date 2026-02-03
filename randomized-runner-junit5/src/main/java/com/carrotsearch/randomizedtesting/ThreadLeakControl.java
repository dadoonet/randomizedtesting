package com.carrotsearch.randomizedtesting;

import com.carrotsearch.randomizedtesting.annotations.SuppressForbidden;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakAction;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakAction.Action;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakFilters;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakGroup;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakLingering;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope.Scope;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakZombies;
import com.carrotsearch.randomizedtesting.annotations.Timeout;
import com.carrotsearch.randomizedtesting.annotations.TimeoutSuite;
import org.opentest4j.TestAbortedException;

import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Logger;

import static com.carrotsearch.randomizedtesting.RandomizedRunnerConstants.*;
import static com.carrotsearch.randomizedtesting.RandomizedTest.systemPropertyAsInt;
import static com.carrotsearch.randomizedtesting.SysGlobals.*;

/**
 * Thread leak control for JUnit 5.
 * 
 * This class provides thread leak detection and control functionality
 * for tests running under the RandomizedExtension.
 */
@SuppressWarnings("resource")
public class ThreadLeakControl {
  /**
   * A dummy class serving as the source of defaults for annotations.
   */
  @ThreadLeakScope
  @ThreadLeakAction
  @ThreadLeakLingering
  @ThreadLeakZombies
  @ThreadLeakFilters
  @ThreadLeakGroup
  private static class DefaultAnnotationValues {
  }

  /**
   * Shared LOGGER.
   */
  private final static Logger LOGGER = logger;

  /**
   * Zombie thread marker.
   */
  static final AtomicBoolean zombieMarker = new AtomicBoolean();

  /**
   * How many attempts to interrupt and then kill a runaway thread before giving up?
   */
  private final int killAttempts;

  /**
   * How long to wait between attempts to kill a runaway thread (millis).
   */
  private final int killWait;

  /**
   * This is the assumed set of threads without leaks.
   */
  private final Set<Thread> expectedSuiteState;

  /**
   * Test timeout.
   */
  private TimeoutValue testTimeout;

  /**
   * Suite timeout.
   */
  private TimeoutValue suiteTimeout;

  /**
   * Built-in filters.
   */
  private final List<ThreadFilter> builtinFilters;

  /**
   * User filter (compound).
   */
  private ThreadFilter suiteFilters;

  /**
   * Thread leak detection group.
   */
  ThreadLeakGroup threadLeakGroup;

  /**
   * Suite timed out flag.
   */
  private final AtomicBoolean suiteTimedOut = new AtomicBoolean();

  /**
   * The thread group for the runner.
   */
  private final ThreadGroup runnerThreadGroup;

  /**
   * Uncaught exception handler.
   */
  private final QueueUncaughtExceptionsHandler handler;

  /**
   * Timeout parsing code and logic.
   */
  private static class TimeoutValue {
    private final int timeoutOverride;
    private final boolean globalTimeoutFirst;

    TimeoutValue(String sysprop, int defaultValue) {
      String timeoutValue = System.getProperty(sysprop);
      boolean globalTimeoutFirst = false;
      if (timeoutValue == null || timeoutValue.trim().length() == 0) {
        timeoutValue = null;
      }
      if (timeoutValue != null) {
        // Check for timeout precedence.
        globalTimeoutFirst = timeoutValue.matches("[0-9]+\\!");
        timeoutValue = timeoutValue.replaceAll("\\!", "");
      } else {
        timeoutValue = Integer.toString(defaultValue);
      }

      this.timeoutOverride = Integer.parseInt(timeoutValue);
      this.globalTimeoutFirst = globalTimeoutFirst;
    }

    int getTimeout(Integer value) {
      if (globalTimeoutFirst) {
        return timeoutOverride;
      } else {
        return value != null ? value : timeoutOverride;
      }
    }
  }

  /**
   * Thread filter for the current thread.
   */
  private static class ThisThreadFilter implements ThreadFilter {
    private final Thread t;

    public ThisThreadFilter(Thread t) {
      this.t = t;
    }

    @Override
    public boolean reject(Thread t) {
      return this.t == t;
    }
  }

  private static ThreadFilter or(final ThreadFilter... filters) {
    return new ThreadFilter() {
      @Override
      public boolean reject(Thread t) {
        boolean reject = false;
        for (ThreadFilter f : filters) {
          if (reject |= f.reject(t)) {
            break;
          }
        }
        return reject;
      }
    };
  }

  /**
   * Filter for known system threads.
   */
  private static class KnownSystemThread implements ThreadFilter {
    @Override
    public boolean reject(Thread t) {
      // Explicit check for system group.
      ThreadGroup tgroup = t.getThreadGroup();
      if (tgroup != null && "system".equals(tgroup.getName()) && tgroup.getParent() == null) {
        return true;
      }

      // Explicit check for Serializer shutdown daemon.
      if (t.getName().equals("JUnit5-serializer-daemon")) {
        return true;
      }

      // Explicit check for java flight recorder (jfr) threads.
      if (t.getName().equals("JFR request timer")) {
        return true;
      }

      // Explicit check for YourKit Java Profiler (YJP) agent thread.
      if (t.getName().equals("YJPAgent-Telemetry")) {
        return true;
      }

      // J9 memory pool thread.
      if (t.getName().equals("MemoryPoolMXBean notification dispatcher")) {
        return true;
      }

      // Explicit check for MacOSX AWT-AppKit
      if (t.getName().equals("AWT-AppKit")) {
        return true;
      }

      // Explicit check for TokenPoller (MessageDigest spawns it).
      if (t.getName().contains("Poller SunPKCS11")) {
        return true;
      }

      // forked process reaper on Unixish systems
      if (t.getName().equals("process reaper")) {
        return true;
      }

      final List<StackTraceElement> stack = new ArrayList<StackTraceElement>(Arrays.asList(getStackTrace(t)));
      Collections.reverse(stack);

      // Explicit check for GC$Daemon
      if (stack.size() >= 1 &&
          stack.get(0).getClassName().startsWith("sun.misc.GC$Daemon")) {
        return true;
      }

      return false;
    }
  }

  /**
   * Constructs a new thread leak control.
   */
  public ThreadLeakControl(ThreadGroup runnerThreadGroup, QueueUncaughtExceptionsHandler handler) {
    this.runnerThreadGroup = runnerThreadGroup;
    this.handler = handler;

    this.killAttempts = systemPropertyAsInt(SYSPROP_KILLATTEMPTS(), DEFAULT_KILLATTEMPTS);
    this.killWait = systemPropertyAsInt(SYSPROP_KILLWAIT(), DEFAULT_KILLWAIT);

    // Determine default timeouts.
    testTimeout = new TimeoutValue(SYSPROP_TIMEOUT(), DEFAULT_TIMEOUT);
    suiteTimeout = new TimeoutValue(SYSPROP_TIMEOUT_SUITE(), DEFAULT_TIMEOUT_SUITE);

    builtinFilters = Arrays.asList(
        new ThisThreadFilter(Thread.currentThread()),
        new KnownSystemThread());

    // Determine a set of expected threads up front (unfiltered).
    expectedSuiteState = Collections.unmodifiableSet(Threads.getAllThreads());
  }

  /**
   * Check on zombie threads status.
   */
  public static void checkZombies() throws TestAbortedException {
    if (hasZombieThreads()) {
      throw new TestAbortedException("Leaked background threads present (zombies).");
    }
  }

  /**
   * Check if there are zombie threads.
   */
  public static boolean hasZombieThreads() {
    return zombieMarker.get();
  }

  /**
   * Initialize suite-level leak control for the given class.
   */
  public void initializeSuite(Class<?> suiteClass) throws Exception {
    threadLeakGroup = firstAnnotated(ThreadLeakGroup.class, suiteClass, DefaultAnnotationValues.class);
    final List<Throwable> errors = new ArrayList<>();
    suiteFilters = instantiateFilters(errors, suiteClass);
    if (!errors.isEmpty()) {
      throw new RuntimeException("Failed to initialize thread filters", errors.get(0));
    }
  }

  /**
   * Get the suite timeout for the given class.
   */
  public int getSuiteTimeout(Class<?> suiteClass) {
    TimeoutSuite timeoutAnn = suiteClass.getAnnotation(TimeoutSuite.class);
    return suiteTimeout.getTimeout(timeoutAnn == null ? null : timeoutAnn.millis());
  }

  /**
   * Get the test timeout for the given method.
   */
  public int getTestTimeout(Class<?> testClass, Method testMethod) {
    Integer timeout = null;

    Timeout timeoutAnn = testClass.getAnnotation(Timeout.class);
    if (timeoutAnn != null) {
      timeout = (int) Math.min(Integer.MAX_VALUE, timeoutAnn.millis());
    }

    // Method-override.
    timeoutAnn = testMethod.getAnnotation(Timeout.class);
    if (timeoutAnn != null) {
      timeout = timeoutAnn.millis();
    }

    return testTimeout.getTimeout(timeout);
  }

  /**
   * Check for thread leaks at the suite level.
   */
  public void checkSuiteLeaks(Class<?> suiteClass, List<Throwable> errors) {
    final AnnotatedElement[] chain = {suiteClass, DefaultAnnotationValues.class};
    checkThreadLeaks(
        refilter(expectedSuiteState, suiteFilters), errors, LifecycleScope.SUITE, suiteClass.getName(), chain);
    processUncaught(errors, handler.getUncaughtAndClear());
  }

  /**
   * Check for thread leaks at the test level.
   */
  public void checkTestLeaks(Class<?> testClass, Method testMethod, List<Throwable> errors) {
    if (suiteTimedOut.get()) {
      return;
    }

    final AnnotatedElement[] chain = {testMethod, testClass, DefaultAnnotationValues.class};
    Set<Thread> beforeTestState = getThreads(suiteFilters);
    checkThreadLeaks(beforeTestState, errors, LifecycleScope.TEST, testMethod.getName(), chain);
    processUncaught(errors, handler.getUncaughtAndClear());
  }

  /**
   * Refilter a set of threads.
   */
  protected Set<Thread> refilter(Set<Thread> in, ThreadFilter f) {
    HashSet<Thread> t = new HashSet<Thread>(in);
    for (Iterator<Thread> i = t.iterator(); i.hasNext(); ) {
      if (f.reject(i.next())) {
        i.remove();
      }
    }
    return t;
  }

  /**
   * Instantiate a full set of {@link ThreadFilter}s for a suite.
   */
  private ThreadFilter instantiateFilters(List<Throwable> errors, Class<?> suiteClass) {
    ThreadLeakFilters ann =
        firstAnnotated(ThreadLeakFilters.class, suiteClass, DefaultAnnotationValues.class);

    final ArrayList<ThreadFilter> filters = new ArrayList<ThreadFilter>();
    for (Class<? extends ThreadFilter> c : ann.filters()) {
      try {
        filters.add(c.getDeclaredConstructor().newInstance());
      } catch (Throwable t) {
        errors.add(t);
      }
    }

    if (ann.defaultFilters()) {
      filters.addAll(builtinFilters);
    }

    return or(filters.toArray(new ThreadFilter[filters.size()]));
  }

  /**
   * Clears a {@link Throwable}'s stack.
   */
  private static <T extends Throwable> T emptyStack(T t) {
    t.setStackTrace(new StackTraceElement[0]);
    return t;
  }

  /**
   * Process uncaught exceptions.
   */
  protected void processUncaught(List<Throwable> errors, List<UncaughtException> uncaughtList) {
    for (UncaughtException e : uncaughtList) {
      errors.add(emptyStack(new UncaughtExceptionError(
          "Captured an uncaught exception in thread: " + e.threadName, e.error)));
    }
  }

  /**
   * Perform a thread leak check at the given scope.
   */
  @SuppressWarnings("deprecation")
  protected void checkThreadLeaks(
      Set<Thread> expectedState,
      List<Throwable> errors,
      LifecycleScope scope, String description,
      AnnotatedElement... annotationChain) {
    final ThreadLeakScope annScope = firstAnnotated(ThreadLeakScope.class, annotationChain);

    // Return immediately if no checking.
    if (annScope.value() == Scope.NONE)
      return;

    // If suite scope check is requested skip testing at test level.
    if (annScope.value() == Scope.SUITE && scope == LifecycleScope.TEST) {
      return;
    }

    // Check for the set of live threads, with optional lingering.
    {
      int lingerTime = firstAnnotated(ThreadLeakLingering.class, annotationChain).linger();
      HashSet<Thread> threads = getThreads(suiteFilters);
      threads.removeAll(expectedState);

      if (lingerTime > 0 && !threads.isEmpty()) {
        final DeadlineClock deadlineClock = new DeadlineClock(TimeUnit.MILLISECONDS, lingerTime);
        try {
          LOGGER.warning("Will linger awaiting termination of " + threads.size() + " leaked thread(s).");
          do {
            Thread.sleep(100);
            threads = getThreads(suiteFilters);
            threads.removeAll(expectedState);
            if (threads.isEmpty() || deadlineClock.isAfterDeadline())
              break;
          } while (true);
        } catch (InterruptedException e) {
          LOGGER.warning("Lingering interrupted.");
        }
      }

      if (threads.isEmpty()) {
        return;
      }
    }

    // Take one more snapshot, this time including stack traces (costly).
    HashMap<Thread, StackTraceElement[]> withTraces = getThreadsWithTraces(suiteFilters);
    withTraces.keySet().removeAll(expectedState);
    if (withTraces.isEmpty()) {
      return;
    }

    // Build up failure message (include stack traces of leaked threads).
    StringBuilder message = new StringBuilder(withTraces.size() + " thread" +
        (withTraces.size() == 1 ? "" : "s") +
        " leaked from " +
        scope + " scope at " + description + ": ");
    message.append(formatThreadStacks(withTraces));

    // The first exception is the "leaked threads" error.
    errors.add(augmentStackTrace(
        emptyStack(new ThreadLeakError(message.toString()))));

    // Perform actions on leaked threads.
    final EnumSet<Action> actions = EnumSet.noneOf(Action.class);
    actions.addAll(Arrays.asList(firstAnnotated(ThreadLeakAction.class, annotationChain).value()));

    if (actions.contains(Action.WARN)) {
      LOGGER.severe(message.toString());
    }

    Set<Thread> zombies = Collections.emptySet();
    if (actions.contains(Action.INTERRUPT)) {
      zombies = tryToInterruptAll(errors, withTraces.keySet());
    }

    // Process zombie thread check consequences here.
    if (!zombies.isEmpty()) {
      switch (firstAnnotated(ThreadLeakZombies.class, annotationChain).value()) {
        case CONTINUE:
          // Do nothing about it.
          break;
        case IGNORE_REMAINING_TESTS:
          // Mark zombie thread presence.
          zombieMarker.set(true);
          break;
        default:
          throw new RuntimeException("Missing case.");
      }
    }
  }

  /**
   * Dump threads and their current stack trace.
   */
  private String formatThreadStacks(Map<Thread, StackTraceElement[]> threads) {
    StringBuilder message = new StringBuilder();
    int cnt = 1;
    final Formatter f = new Formatter(message, Locale.ROOT);
    for (Map.Entry<Thread, StackTraceElement[]> e : threads.entrySet()) {
      f.format(Locale.ROOT, "\n  %2d) %s", cnt++, Threads.threadName(e.getKey())).flush();
      if (e.getValue().length == 0) {
        message.append("\n        at (empty stack)");
      } else {
        for (StackTraceElement ste : e.getValue()) {
          message.append("\n        at ").append(ste);
        }
      }
    }
    return message.toString();
  }

  /**
   * Collect thread names.
   */
  private String threadNames(java.util.Collection<Thread> threads) {
    StringBuilder b = new StringBuilder();
    final Formatter f = new Formatter(b, Locale.ROOT);
    int cnt = 1;
    for (Thread t : threads) {
      f.format(Locale.ROOT, "\n  %2d) %s", cnt++, Threads.threadName(t));
    }
    return b.toString();
  }

  private static StackTraceElement[] getStackTrace(final Thread t) {
    return AccessController.doPrivileged(new PrivilegedAction<StackTraceElement[]>() {
      @Override
      public StackTraceElement[] run() {
        return t.getStackTrace();
      }
    });
  }

  /**
   * Returns all {@link ThreadLeakGroup} applicable threads, with stack
   * traces, for analysis.
   */
  private HashMap<Thread, StackTraceElement[]> getThreadsWithTraces(ThreadFilter... filters) {
    final Set<Thread> threads = getThreads(filters);
    final HashMap<Thread, StackTraceElement[]> r = new HashMap<Thread, StackTraceElement[]>();
    for (Thread t : threads) {
      r.put(t, getStackTrace(t));
    }
    return r;
  }

  /**
   * Returns all {@link ThreadLeakGroup} threads for analysis.
   */
  private HashSet<Thread> getThreads(ThreadFilter... filters) {
    HashSet<Thread> threads;
    switch (threadLeakGroup.value()) {
      case ALL:
        threads = Threads.getAllThreads();
        break;
      case MAIN:
        threads = Threads.getThreads(mainThreadGroup);
        break;
      case TESTGROUP:
        threads = Threads.getThreads(runnerThreadGroup);
        break;
      default:
        throw new RuntimeException();
    }

    final ThreadFilter filter = or(filters);
    for (Iterator<Thread> i = threads.iterator(); i.hasNext(); ) {
      Thread t = i.next();
      if (!t.isAlive() || filter.reject(t)) {
        i.remove();
      }
    }

    return threads;
  }

  /**
   * Attempt to interrupt all threads in the given set.
   */
  private Set<Thread> tryToInterruptAll(List<Throwable> errors, Set<Thread> threads) {
    LOGGER.info("Starting to interrupt leaked threads:" + threadNames(threads));

    // stop reporting uncaught exceptions.
    handler.stopReporting();
    try {
      final HashSet<Thread> ordered = new HashSet<Thread>(threads);

      int interruptAttempts = this.killAttempts;
      int interruptWait = this.killWait;
      boolean allDead;
      final int restorePriority = Thread.currentThread().getPriority();
      do {
        allDead = true;
        try {
          Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
          for (Thread t : ordered) {
            t.interrupt();
          }

          // Maximum wait time.
          DeadlineClock waitDeadlineClock = new DeadlineClock(TimeUnit.MILLISECONDS, interruptWait);
          for (Iterator<Thread> i = ordered.iterator(); i.hasNext(); ) {
            final Thread t = i.next();
            if (t.isAlive()) {
              allDead = false;
              join(t, Math.max(1, waitDeadlineClock.timeUntilDeadline(TimeUnit.MILLISECONDS)));
            } else {
              i.remove();
            }
          }
        } catch (InterruptedException e) {
          interruptAttempts = 0;
        }
      } while (!allDead && --interruptAttempts > 0);
      Thread.currentThread().setPriority(restorePriority);

      // Check after the last join.
      HashMap<Thread, StackTraceElement[]> zombies = new HashMap<Thread, StackTraceElement[]>();
      for (Thread t : ordered) {
        if (t.isAlive()) {
          zombies.put(t, getStackTrace(t));
        }
      }

      if (zombies.isEmpty()) {
        LOGGER.info("All leaked threads terminated.");
      } else {
        String message = "There are still zombie threads that couldn't be terminated:" + formatThreadStacks(zombies);
        LOGGER.severe(message);
        errors.add(augmentStackTrace(
            emptyStack(new ThreadLeakError(message.toString()))));
      }

      return zombies.keySet();
    } finally {
      handler.resumeReporting();
    }
  }

  static void join(Thread t, long millis) throws InterruptedException {
    if (millis <= 0) {
      throw new IllegalArgumentException("Timeout must be positive: " + millis);
    }

    DeadlineClock deadlineClock = new DeadlineClock(TimeUnit.MILLISECONDS, millis);
    while (t.isAlive()) {
      long untilDeadline = deadlineClock.timeUntilDeadline(TimeUnit.MILLISECONDS);
      if (untilDeadline > 0) {
        Thread.sleep(Math.min(250, untilDeadline));
      } else {
        break;
      }
    }
  }

  public boolean isTimedOut() {
    return suiteTimedOut.get();
  }

  public void markSuiteTimedOut() {
    suiteTimedOut.set(true);
  }

  /**
   * Returns an annotation's instance declared on any annotated element (first one wins)
   * or the default value if not present on any of them.
   */
  private static <T extends Annotation> T firstAnnotated(Class<T> clazz, AnnotatedElement... elements) {
    for (AnnotatedElement element : elements) {
      T ann = element.getAnnotation(clazz);
      if (ann != null) return ann;
    }
    throw new RuntimeException("default annotation value must be within elements.");
  }
}

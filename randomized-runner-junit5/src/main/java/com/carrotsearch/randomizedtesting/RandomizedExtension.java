package com.carrotsearch.randomizedtesting;

import static com.carrotsearch.randomizedtesting.SysGlobals.*;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.opentest4j.TestAbortedException;

import com.carrotsearch.randomizedtesting.annotations.*;
import com.carrotsearch.randomizedtesting.extensions.TestGroupCondition;

/**
 * A JUnit 5 Jupiter {@link Extension} for running randomized test cases with
 * predictable and repeatable randomness.
 *
 * <p>This extension is the JUnit 5 equivalent of the JUnit 4 {@code RandomizedRunner}.
 * It provides the same functionality including:
 * <ul>
 *   <li>Predictable random seeds for test reproducibility</li>
 *   <li>Test group filtering via {@link TestGroup} annotations</li>
 *   <li>Lifecycle hooks with randomized ordering</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * {@literal @}ExtendWith(RandomizedExtension.class)
 * public class MyTest {
 *   {@literal @}Test
 *   public void testSomething() {
 *     // Use RandomizedContext.current().getRandom() for randomness
 *   }
 * }
 * </pre>
 *
 * @see RandomizedTest
 * @see RandomizedContext
 * @see TestGroup
 */
public class RandomizedExtension implements
    BeforeAllCallback,
    AfterAllCallback,
    BeforeEachCallback,
    AfterEachCallback,
    TestExecutionExceptionHandler,
    InvocationInterceptor,
    TestInstancePostProcessor,
    ExecutionCondition {
  
  private final TestGroupCondition testGroupCondition = new TestGroupCondition();

  /**
   * Fake package of a stack trace entry inserted into exceptions thrown by
   * test methods. These stack entries contain additional information about
   * seeds used during execution.
   */
  public static final String AUGMENTED_SEED_PACKAGE = "__randomizedtesting";

  /**
   * Default timeout for a single test case (disabled by default).
   */
  public static final int DEFAULT_TIMEOUT = 0;

  /**
   * Default timeout for an entire suite (disabled by default).
   */
  public static final int DEFAULT_TIMEOUT_SUITE = 0;

  /**
   * The default number of first interrupts, then Thread.stop attempts.
   */
  public static final int DEFAULT_KILLATTEMPTS = 5;

  /**
   * Time in between interrupt retries or stop retries.
   */
  public static final int DEFAULT_KILLWAIT = 500;

  /**
   * The default number of test repeat iterations.
   */
  public static final int DEFAULT_ITERATIONS = 1;

  private static final Namespace NAMESPACE = Namespace.create(RandomizedExtension.class);

  private static final String KEY_CONTEXT = "randomizedContext";
  private static final String KEY_RANDOMNESS = "randomness";
  private static final String KEY_RUNNER_RANDOMNESS = "runnerRandomness";
  private static final String KEY_CLASS_MODEL = "classModel";
  private static final String KEY_THREAD_GROUP = "threadGroup";
  private static final String KEY_HANDLER = "uncaughtHandler";

  /**
   * Package scope logger.
   */
  static final Logger logger = Logger.getLogger(RandomizedExtension.class.getSimpleName());

  /**
   * A sequencer for affecting the initial seed in case of rapid succession of this class
   * instance creations.
   */
  private static final AtomicLong sequencer = new AtomicLong();

  private static final List<String> DEFAULT_STACK_FILTERS = Arrays.asList(
      "org.junit.",
      "sun.",
      "java.lang.reflect.",
      "com.carrotsearch.randomizedtesting."
  );

  /**
   * A marker for flagging zombie threads (leaked threads that couldn't be killed).
   */
  static AtomicBoolean zombieMarker = new AtomicBoolean(false);

  /**
   * The "main" thread group we will be tracking (including subgroups).
   */
  static final ThreadGroup mainThreadGroup = Thread.currentThread().getThreadGroup();

  public RandomizedExtension() {
    // Default constructor
  }

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    return testGroupCondition.evaluateExecutionCondition(context);
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    Class<?> testClass = context.getRequiredTestClass();
    Store store = context.getStore(NAMESPACE);

    // Initialize the runner's main seed/randomness source
    Randomness runnerRandomness = initializeRandomness(testClass);
    store.put(KEY_RUNNER_RANDOMNESS, runnerRandomness);

    // Create class model
    ClassModel classModel = new ClassModel(testClass);
    store.put(KEY_CLASS_MODEL, classModel);

    // Use the current thread's thread group for context registration
    // This is important because JUnit 5 runs tests in the main thread group
    ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
    store.put(KEY_THREAD_GROUP, threadGroup);

    // Set up uncaught exception handler
    QueueUncaughtExceptionsHandler handler = new QueueUncaughtExceptionsHandler();
    UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
    AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
      Thread.setDefaultUncaughtExceptionHandler(handler);
      return null;
    });
    store.put(KEY_HANDLER, handler);
    store.put("previousHandler", previous);

    // Create and register context
    RandomizedContext randomizedContext = RandomizedContext.create(threadGroup, testClass, runnerRandomness);
    store.put(KEY_CONTEXT, randomizedContext);

    // Push class-level randomness
    Randomness classRandomness = runnerRandomness.clone(Thread.currentThread());
    randomizedContext.push(classRandomness);

    // Set thread name
    Thread.currentThread().setName("SUITE-" + Classes.simpleName(testClass) +
        "-seed#" + SeedUtils.formatSeedChain(runnerRandomness));
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    Store store = context.getStore(NAMESPACE);

    // Close context resources
    RandomizedContext randomizedContext = store.get(KEY_CONTEXT, RandomizedContext.class);
    if (randomizedContext != null) {
      closeContextResources(randomizedContext, LifecycleScope.SUITE);
      randomizedContext.popAndDestroy();
      randomizedContext.dispose();
    }

    // Restore uncaught exception handler
    UncaughtExceptionHandler previous = store.get("previousHandler", UncaughtExceptionHandler.class);
    AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
      Thread.setDefaultUncaughtExceptionHandler(previous);
      return null;
    });
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    Store store = context.getStore(NAMESPACE);
    RandomizedContext randomizedContext = store.get(KEY_CONTEXT, RandomizedContext.class);
    Randomness runnerRandomness = store.get(KEY_RUNNER_RANDOMNESS, Randomness.class);

    if (randomizedContext != null && runnerRandomness != null) {
      Method testMethod = context.getRequiredTestMethod();

      // Determine test seed - use the same RandomSupplier as the runner
      long testSeed = determineTestSeed(testMethod, runnerRandomness);
      Randomness testRandomness = new Randomness(testSeed, runnerRandomness.getRandomSupplier());
      store.put(KEY_RANDOMNESS, testRandomness);

      // Push test-level randomness
      randomizedContext.push(testRandomness);
      randomizedContext.setTargetMethod(testMethod);

      // Set thread name for better debugging
      Class<?> testClass = context.getRequiredTestClass();
      Thread.currentThread().setName("TEST-" + Classes.simpleName(testClass) +
          "." + testMethod.getName() + "-seed#" + SeedUtils.formatSeedChain(runnerRandomness, testRandomness));
    }
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    Store store = context.getStore(NAMESPACE);
    RandomizedContext randomizedContext = store.get(KEY_CONTEXT, RandomizedContext.class);

    if (randomizedContext != null) {
      // Close test-level resources
      closeContextResources(randomizedContext, LifecycleScope.TEST);

      // Reset target method and pop randomness
      randomizedContext.setTargetMethod(null);
      randomizedContext.popAndDestroy();
    }
  }

  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
    // Instance post-processing if needed
    // This can be used to inject dependencies or perform setup
  }

  @Override
  public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
    Store store = context.getStore(NAMESPACE);
    Randomness runnerRandomness = store.get(KEY_RUNNER_RANDOMNESS, Randomness.class);
    Randomness testRandomness = store.get(KEY_RANDOMNESS, Randomness.class);

    // Augment stack trace with seed information
    if (runnerRandomness != null) {
      Randomness[] seeds = testRandomness != null ?
          new Randomness[]{runnerRandomness, testRandomness} :
          new Randomness[]{runnerRandomness};
      augmentStackTrace(throwable, seeds);
    }

    throw throwable;
  }

  @Override
  public void interceptTestMethod(Invocation<Void> invocation,
                                  ReflectiveInvocationContext<Method> invocationContext,
                                  ExtensionContext extensionContext) throws Throwable {
    // Execute the test method with potential timeout handling
    invocation.proceed();
  }

  /**
   * Initialize randomness for the test class.
   */
  private Randomness initializeRandomness(Class<?> testClass) {
    List<SeedDecorator> decorators = new ArrayList<>();
    for (SeedDecorators decAnn : getAnnotationsFromClassHierarchy(testClass, SeedDecorators.class)) {
      for (Class<? extends SeedDecorator> clazz : decAnn.value()) {
        try {
          SeedDecorator dec = clazz.getDeclaredConstructor().newInstance();
          dec.initialize(testClass);
          decorators.add(dec);
        } catch (Throwable t) {
          throw new RuntimeException("Could not initialize suite class: "
              + testClass.getName() + " because its @SeedDecorators contains non-instantiable: "
              + clazz.getName(), t);
        }
      }
    }
    SeedDecorator[] decArray = decorators.toArray(new SeedDecorator[0]);

    RandomSupplier randomSupplier = determineRandomSupplier(testClass);

    final long randomSeed = MurmurHash3.hash(sequencer.getAndIncrement() + System.nanoTime());
    final String globalSeed = emptyToNull(System.getProperty(SYSPROP_RANDOM_SEED()));
    final long initialSeed;

    if (globalSeed != null) {
      final long[] seedChain = SeedUtils.parseSeedChain(globalSeed);
      if (seedChain.length == 0 || seedChain.length > 2) {
        throw new IllegalArgumentException("Invalid system property "
            + SYSPROP_RANDOM_SEED() + " specification: " + globalSeed);
      }
      initialSeed = seedChain[0];
    } else if (testClass.isAnnotationPresent(Seed.class)) {
      initialSeed = seedFromAnnot(testClass, randomSeed)[0];
    } else {
      initialSeed = randomSeed;
    }

    return new Randomness(initialSeed, randomSupplier, decArray);
  }

  /**
   * Determine the random supplier from class annotations.
   */
  private RandomSupplier determineRandomSupplier(Class<?> testClass) {
    List<TestContextRandomSupplier> randomImpl =
        getAnnotationsFromClassHierarchy(testClass, TestContextRandomSupplier.class);
    if (randomImpl.isEmpty()) {
      return RandomSupplier.DEFAULT;
    } else {
      Class<? extends RandomSupplier> clazz = randomImpl.get(randomImpl.size() - 1).value();
      try {
        return clazz.getDeclaredConstructor().newInstance();
      } catch (Exception e) {
        throw new IllegalArgumentException("Could not instantiate random supplier of class: " + clazz, e);
      }
    }
  }

  /**
   * Determine the seed for a test method.
   */
  private long determineTestSeed(Method method, Randomness runnerRandomness) {
    // Check for method-level @Seed annotation
    Seed seed = method.getAnnotation(Seed.class);
    if (seed != null && !seed.value().equals("random")) {
      long[] seeds = SeedUtils.parseSeedChain(seed.value());
      if (seeds.length > 0) {
        return seeds[0];
      }
    }

    // Default: derive from runner seed and method name
    return runnerRandomness.getSeed() ^ MurmurHash3.hash((long) method.getName().hashCode());
  }

  /**
   * Close resources registered in the context for the given scope.
   */
  private void closeContextResources(RandomizedContext context, LifecycleScope scope) {
    context.closeResources(info -> {
      try {
        info.getResource().close();
      } catch (Throwable t) {
        logger.log(Level.WARNING, "Resource failed to close: " + info, t);
      }
    }, scope);
  }

  /**
   * Augment stack trace with seed information.
   */
  static <T extends Throwable> T augmentStackTrace(T e, Randomness... seeds) {
    if (seeds.length == 0) {
      return e;
    }

    final String seedChain = SeedUtils.formatSeedChain(seeds);
    final String existingSeed = seedFromThrowable(e);
    if (existingSeed != null && existingSeed.equals(seedChain)) {
      return e;
    }

    List<StackTraceElement> stack = new ArrayList<>(Arrays.asList(e.getStackTrace()));
    stack.add(0, new StackTraceElement(AUGMENTED_SEED_PACKAGE + ".SeedInfo",
        "seed", seedChain, 0));
    e.setStackTrace(stack.toArray(new StackTraceElement[0]));
    return e;
  }

  /**
   * Extract seed from throwable's augmented stack trace.
   */
  public static String seedFromThrowable(Throwable t) {
    StringBuilder b = new StringBuilder();
    while (t != null) {
      for (StackTraceElement s : t.getStackTrace()) {
        if (s.getClassName().startsWith(AUGMENTED_SEED_PACKAGE)) {
          if (b.length() > 0) b.append(", ");
          b.append(s.getFileName());
        }
      }
      t = t.getCause();
    }
    return b.length() == 0 ? null : b.toString();
  }

  /**
   * Collect annotations from class hierarchy.
   */
  private static <T extends Annotation> List<T> getAnnotationsFromClassHierarchy(
      Class<?> clazz, Class<T> annotation) {
    List<T> anns = new ArrayList<>();
    IdentityHashMap<T, T> inherited = new IdentityHashMap<>();
    for (Class<?> c = clazz; c != Object.class; c = c.getSuperclass()) {
      if (c.isAnnotationPresent(annotation)) {
        T ann = c.getAnnotation(annotation);
        if (ann.annotationType().isAnnotationPresent(Inherited.class) &&
            inherited.containsKey(ann)) {
          continue;
        }
        anns.add(ann);
        inherited.put(ann, ann);
      }
    }
    Collections.reverse(anns);
    return anns;
  }

  /**
   * Get seed from annotation.
   */
  private long[] seedFromAnnot(AnnotatedElement element, long randomSeed) {
    Seed seed = element.getAnnotation(Seed.class);
    String seedChain = seed.value();
    if (seedChain.equals("random")) {
      return new long[]{randomSeed};
    }
    return SeedUtils.parseSeedChain(seedChain);
  }

  /**
   * Normalize empty strings to nulls.
   */
  static String emptyToNull(String value) {
    if (value == null || value.trim().isEmpty())
      return null;
    return value.trim();
  }

  /**
   * Returns true if any previous (or current) suite has left zombie threads.
   */
  public static boolean hasZombieThreads() {
    return zombieMarker.get();
  }

  /**
   * Queue uncaught exceptions handler.
   */
  static class QueueUncaughtExceptionsHandler implements UncaughtExceptionHandler {
    private final ArrayList<UncaughtException> uncaughtExceptions = new ArrayList<>();
    private boolean reporting = true;

    @Override
    public void uncaughtException(Thread t, Throwable e) {
      synchronized (this) {
        if (!reporting) {
          return;
        }
        uncaughtExceptions.add(new UncaughtException(t, e));
      }

      Logger.getLogger(RunnerThreadGroup.class.getSimpleName()).log(
          Level.WARNING,
          "Uncaught exception in thread: " + t, e);
    }

    void stopReporting() {
      synchronized (this) {
        reporting = false;
      }
    }

    void resumeReporting() {
      synchronized (this) {
        reporting = true;
      }
    }

    public List<UncaughtException> getUncaughtAndClear() {
      synchronized (this) {
        final ArrayList<UncaughtException> copy = new ArrayList<>(uncaughtExceptions);
        uncaughtExceptions.clear();
        return copy;
      }
    }
  }

  /**
   * Uncaught exception holder.
   */
  static class UncaughtException {
    final Thread thread;
    final String threadName;
    final Throwable error;

    UncaughtException(Thread t, Throwable error) {
      this.threadName = Threads.threadName(t);
      this.thread = t;
      this.error = error;
    }
  }
}

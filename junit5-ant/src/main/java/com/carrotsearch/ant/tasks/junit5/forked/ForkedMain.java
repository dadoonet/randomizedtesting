package com.carrotsearch.ant.tasks.junit5.forked;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import com.carrotsearch.ant.tasks.junit5.events.AppendStdErrEvent;
import com.carrotsearch.ant.tasks.junit5.events.AppendStdOutEvent;
import com.carrotsearch.ant.tasks.junit5.events.BootstrapEvent;
import com.carrotsearch.ant.tasks.junit5.events.Serializer;
import com.carrotsearch.ant.tasks.junit5.events.SuiteFailureEvent;
import com.carrotsearch.ant.tasks.junit5.events.mirrors.TestDescriptionMirror;
import com.carrotsearch.randomizedtesting.SysGlobals;
import com.carrotsearch.randomizedtesting.annotations.SuppressForbidden;
import com.google.common.base.Strings;
import com.google.common.collect.Iterators;

/**
 * A forked JVM process running the actual tests on the target JVM using JUnit Platform.
 */
public class ForkedMain {
  /** Runtime exception. */
  public static final int ERR_EXCEPTION = 240;

  /** No JUnit on classpath. */
  public static final int ERR_NO_JUNIT = 239;

  /** Old JUnit on classpath. */
  public static final int ERR_OLD_JUNIT = 238;

  /** OOM */
  public static final int ERR_OOM = 237;

  /**
   * Last resort memory pool released under low memory conditions.
   */
  static volatile Object lastResortMemory = new byte [1024 * 1024 * 5];

  /**
   * Preallocate and load in advance. 
   */
  static Class<OutOfMemoryError> oomClass = OutOfMemoryError.class;
  
  /**
   * Frequent event stream flushing.
   */
  public static final String OPTION_FREQUENT_FLUSH = "-flush";

  /**
   * Multiplex sysout and syserr to original streams.
   */
  public static final String OPTION_SYSOUTS = "-sysouts";

  /**
   * Read class names from standard input.
   */
  public static final String OPTION_STDIN = "-stdin";

  /**
   * Name the sink for events.
   */
  public static final String OPTION_EVENTSFILE = "-eventsfile";

  /**
   * Should the debug stream from the runner be created?
   */
  public static final String OPTION_DEBUGSTREAM = "-debug";

  /**
   * User-defined TestExecutionListener classes.
   */
  public static final String OPTION_LISTENERS = "-listeners";

  /**
   * Fire a runner failure after startup to verify messages are propagated properly.
   */
  public static final String SYSPROP_FIRERUNNERFAILURE =
      ForkedMain.class.getName() + ".fireRunnerFailure";

  /**
   * Delay the initial bootstrap event from the forked JVM (used in tests).
   */
  public static final String SYSPROP_FORKEDJVM_DELAY_MS =
      "junit5.tests.internal.initialDelayMs";

  /**
   * Event sink.
   */
  private final Serializer serializer;

  /** A sink for warnings (non-event stream). */
  private static PrintStream warnings;

  /** Flush serialization stream frequently. */
  private boolean flushFrequently = false;

  /** Debug stream to flush progress information to. */
  private File debugMessagesFile;

  /** List of TestExecutionListener classes */
  private String listeners;

  /** 
   * Multiplex calls to System streams to both event stream and the original streams?
   */
  private static boolean multiplexStdStreams = false;

  /**
   * Base for redirected streams. 
   */
  private static class ChunkedStream extends OutputStream {
    public void write(int b) throws IOException {
      throw new IOException("Only buffered write(byte[],int,int) calls expected from super stream.");
    }
    
    @Override
    public void close() throws IOException {
      throw new IOException("Not supposed to be called on redirected streams.");
    }
  }

  /**
   * Creates a forked JVM emitting events to the given serializer.
   */
  public ForkedMain(Serializer serializer) {
    this.serializer = serializer;
  }

  /**
   * Execute tests using JUnit Platform Launcher.
   */
  private void execute(Iterator<String> classNames) throws Throwable {
    final Writer debug = debugMessagesFile == null ? new NullWriter() : 
        new OutputStreamWriter(new FileOutputStream(debugMessagesFile), "UTF-8");

    // Create the launcher
    Launcher launcher = LauncherFactory.create();

    // Create the main emitter
    TestExecutionEmitter emitter = new TestExecutionEmitter(serializer);
    
    // Debug listener for flush and logging
    TestExecutionListener debugListener = createDebugListener(debug);

    // Method filter from system property
    String methodFilterGlob = Strings.emptyToNull(System.getProperty(SysGlobals.SYSPROP_TESTMETHOD()));

    debug(debug, "Entering main suite loop.");
    try {
      while (classNames.hasNext()) {
        final String clName = classNames.next();
        debug(debug, "Discovering: " + clName);
        
        Class<?> clazz = instantiate(clName);
        if (clazz == null) 
          continue;

        // Build discovery request for this class
        LauncherDiscoveryRequestBuilder requestBuilder = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectClass(clazz));
        
        // Add method filter if specified
        if (methodFilterGlob != null) {
          final String methodPattern = convertGlobToRegex(methodFilterGlob);
          requestBuilder.filters((PostDiscoveryFilter) testDescriptor -> {
            // Only filter test methods, not containers
            if (testDescriptor.isContainer()) {
              return FilterResult.included("container");
            }
            TestSource source = testDescriptor.getSource().orElse(null);
            if (source instanceof MethodSource) {
              String methodName = ((MethodSource) source).getMethodName();
              if (methodName.matches(methodPattern)) {
                return FilterResult.included("matches pattern");
              }
              return FilterResult.excluded("does not match pattern: " + methodPattern);
            }
            return FilterResult.included("no method source");
          });
        }

        LauncherDiscoveryRequest request = requestBuilder.build();

        // Discover tests
        TestPlan testPlan = launcher.discover(request);
        
        if (!testPlan.containsTests()) {
          debug(debug, "No tests found in: " + clName);
          continue;
        }

        // Register listeners
        List<TestExecutionListener> allListeners = new ArrayList<>();
        allListeners.add(emitter);
        allListeners.add(debugListener);
        allListeners.addAll(instantiateListeners());

        debug(debug, "Executing: " + clName);
        launcher.execute(request, allListeners.toArray(new TestExecutionListener[0]));
        debug(debug, "Done: " + clName);
      }
    } catch (Throwable t) {
      debug(debug, "Main suite loop error: " + t);
      throw t;
    } finally {
      debug(debug, "Leaving main suite loop.");
      debug.close();
    }
  }

  /**
   * Convert glob pattern to regex for JUnit Platform.
   */
  private String convertGlobToRegex(String glob) {
    StringBuilder regex = new StringBuilder();
    for (char c : glob.toCharArray()) {
      switch (c) {
        case '*':
          regex.append(".*");
          break;
        case '?':
          regex.append(".");
          break;
        case '.':
        case '(':
        case ')':
        case '[':
        case ']':
        case '{':
        case '}':
        case '\\':
        case '^':
        case '$':
        case '|':
        case '+':
          regex.append("\\").append(c);
          break;
        default:
          regex.append(c);
      }
    }
    return regex.toString();
  }

  /**
   * Create a debug listener that flushes and logs events.
   */
  private TestExecutionListener createDebugListener(final Writer debug) {
    return new TestExecutionListener() {
      @Override
      public void testPlanExecutionStarted(TestPlan testPlan) {
        try {
          debug(debug, "testPlanExecutionStarted");
          serializer.flush();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void testPlanExecutionFinished(TestPlan testPlan) {
        try {
          debug(debug, "testPlanExecutionFinished");
          serializer.flush();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void executionStarted(TestIdentifier testIdentifier) {
        try {
          debug(debug, "executionStarted(" + testIdentifier.getDisplayName() + ")");
          serializer.flush();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void executionFinished(TestIdentifier testIdentifier, 
          org.junit.platform.engine.TestExecutionResult result) {
        try {
          debug(debug, "executionFinished(" + testIdentifier.getDisplayName() + ", " + result.getStatus() + ")");
          serializer.flush();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        try {
          debug(debug, "executionSkipped(" + testIdentifier.getDisplayName() + ", " + reason + ")");
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  private void debug(Writer w, String msg) throws IOException {
    w.write(msg);
    w.write("\n");
    w.flush();
  }

  /**
   * Instantiate test classes (or try to).
   */
  private Class<?> instantiate(String className) {
    try {
      return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
    } catch (Throwable t) {
      try {
        serializer.serialize(
            new SuiteFailureEvent(
                TestDescriptionMirror.createSuiteDescription(className), t));
        if (flushFrequently)
          serializer.flush();
      } catch (Exception e) {
        warn("Could not report failure back to main JVM.", t);
      }
      return null;
    }
  }

  /**
   * Console entry point.
   */
  @SuppressWarnings("resource")
  public static void main(String[] allArgs) {
    int exitStatus = 0;

    Serializer serializer = null;
    try {
      final ArrayDeque<String> args = new ArrayDeque<String>(Arrays.asList(allArgs));

      // Options.
      boolean debugStream = false;
      boolean flushFrequently = false;
      File eventsFile = null;
      boolean suitesOnStdin = false;
      List<String> testClasses = new ArrayList<>();
      String listeners = null;

      while (!args.isEmpty()) {
        String option = args.pop();
        if (option.equals(OPTION_FREQUENT_FLUSH)) {
          flushFrequently = true;
        } else if (option.equals(OPTION_STDIN)) {
          suitesOnStdin = true;
        } else if (option.equals(OPTION_SYSOUTS)) {
          multiplexStdStreams = true;
        } else if (option.equals(OPTION_EVENTSFILE)) {
          eventsFile = new File(args.pop());
          if (eventsFile.isFile() && eventsFile.length() > 0) {
            RandomAccessFile raf = new RandomAccessFile(eventsFile, "rw");
            raf.setLength(0);
            raf.close();
          }
        } else if (option.equals(OPTION_LISTENERS)) {
          listeners = args.pop();
        } else if (option.startsWith(OPTION_DEBUGSTREAM)) {
          debugStream = true;
        } else if (option.startsWith("@")) {
          // Append arguments file, one line per option.
          args.addAll(Arrays.asList(readArgsFile(option.substring(1))));
        } else {
          // The default expectation is a test class.
          testClasses.add(option);
        }
      }

      // Set up events channel and events serializer.
      if (eventsFile == null) {
        throw new IOException("You must specify communication channel for events.");
      }

      // Delay the forked JVM a bit (for tests).
      if (System.getProperty(SYSPROP_FORKEDJVM_DELAY_MS) != null) {
        Thread.sleep(Integer.parseInt(System.getProperty(SYSPROP_FORKEDJVM_DELAY_MS)));
      }
      
      // Send bootstrap package.
      serializer = new Serializer(new EventsOutputStream(eventsFile))
        .serialize(new BootstrapEvent())
        .flush();

      // Redirect original streams and start running tests.
      redirectStreams(serializer, flushFrequently);

      final ForkedMain main = new ForkedMain(serializer);
      main.flushFrequently = flushFrequently;
      main.debugMessagesFile = debugStream ? new File(eventsFile.getAbsolutePath() + ".debug"): null;
      main.listeners = listeners;

      final Iterator<String> stdInput;
      if (suitesOnStdin) { 
        stdInput = new StdInLineIterator(main.serializer);
      } else {
        stdInput = Collections.<String>emptyList().iterator();
      }

      main.execute(Iterators.concat(testClasses.iterator(), stdInput));
      
      // For unhandled exceptions tests.
      if (System.getProperty(SYSPROP_FIRERUNNERFAILURE) != null) {
        throw new Exception(System.getProperty(SYSPROP_FIRERUNNERFAILURE));
      }
    } catch (Throwable t) {
      lastResortMemory = null;
      tryWaitingForGC();

      if (t.getClass() == oomClass) {
        exitStatus = ERR_OOM;
        warn("JVM out of memory.", t);
      } else {
        exitStatus = ERR_EXCEPTION;
        warn("Exception at main loop level.", t);
      }
    }

    try {
      if (serializer != null) {
        try {
          serializer.close();
        } catch (Throwable t) {
          warn("Exception closing serializer.", t);
        }
      }
    } finally {
      JvmExit.halt(exitStatus);
    }
  }

  /**
   * Try waiting for a GC to happen.
   */
  private static void tryWaitingForGC() {
    final long duration = TimeUnit.SECONDS.toNanos(2);
    final long startTime = System.nanoTime();
    while (System.nanoTime() - startTime < duration) {
      System.gc(); 
      try {
        Thread.sleep(250);
      } catch (InterruptedException e) {
        break;
      }
    }
  }

  /**
   * Read arguments from a file.
   */
  private static String[] readArgsFile(String argsFile) throws IOException {
    final ArrayList<String> lines = new ArrayList<String>();
    final BufferedReader reader = new BufferedReader(
        new InputStreamReader(
            new FileInputStream(argsFile), "UTF-8"));
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (!line.isEmpty() && !line.startsWith("#")) {
          lines.add(line);
        }
      }
    } finally {
      reader.close();
    }
    return lines.toArray(new String [lines.size()]);
  }

  /**
   * Redirect standard streams so that the output can be passed to listeners.
   */
  @SuppressForbidden("legitimate sysstreams.")
  private static void redirectStreams(final Serializer serializer, final boolean flushFrequently) {
    final PrintStream origSysOut = System.out;
    final PrintStream origSysErr = System.err;

    // Set warnings stream to System.err.
    warnings = System.err;
    AccessController.doPrivileged(new PrivilegedAction<Void>() {
      @SuppressForbidden("legitimate PrintStream with default charset.")
      @Override
      public Void run() {
        System.setOut(new PrintStream(new BufferedOutputStream(new ChunkedStream() {
          @Override
          public void write(byte[] b, int off, int len) throws IOException {
            if (multiplexStdStreams) {
              origSysOut.write(b, off, len);
            }
            serializer.serialize(new AppendStdOutEvent(b, off, len));
            if (flushFrequently) serializer.flush();
          }
        })));

        System.setErr(new PrintStream(new BufferedOutputStream(new ChunkedStream() {
          @Override
          public void write(byte[] b, int off, int len) throws IOException {
            if (multiplexStdStreams) {
              origSysErr.write(b, off, len);
            }
            serializer.serialize(new AppendStdErrEvent(b, off, len));
            if (flushFrequently) serializer.flush();
          }
        })));
        return null;
      }
    });
  }

  /**
   * Warning emitter.
   */
  @SuppressForbidden("legitimate sysstreams.")
  public static void warn(String message, Throwable t) {
    PrintStream w = (warnings == null ? System.err : warnings);
    try {
      w.print("WARN: ");
      w.print(message);
      if (t != null) {
        w.print(" -> ");
        try {
          t.printStackTrace(w);
        } catch (OutOfMemoryError e) {
          w.print(t.getClass().getName());
          w.print(": ");
          w.print(t.getMessage());
          w.println(" (stack unavailable; OOM)");
        }
      } else {
        w.println();
      }
      w.flush();
    } catch (OutOfMemoryError t2) {
      w.println("ERROR: Couldn't even serialize a warning (out of memory).");
    } catch (Throwable t2) {
      w.println("ERROR: Couldn't even serialize a warning.");
    }
  }

  /**
   * Instantiate TestExecutionListener instances for any user defined listeners.
   */
  private List<TestExecutionListener> instantiateListeners() throws Exception {
    List<TestExecutionListener> instances = new ArrayList<>();

    if (listeners != null) {
      for (String className : Arrays.asList(listeners.split(","))) {
        Class<?> clazz = instantiate(className);
        if (clazz != null) {
          instances.add((TestExecutionListener) clazz.newInstance());
        }
      }
    }

    return instances;
  }
}

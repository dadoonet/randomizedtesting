package com.carrotsearch.randomizedtesting;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.opentest4j.TestAbortedException;

import com.carrotsearch.randomizedtesting.extensions.SystemPropertiesInvariantExtension;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Utility class to surround nested {@link RandomizedExtension} test suites.
 */
@ExtendWith(RandomizedExtension.class)
public class WithNestedTestClass {
  private static boolean runningNested;

  public enum Place {
    CLASS_RULE,
    BEFORE_CLASS,
    CONSTRUCTOR,
    TEST_RULE,
    BEFORE,
    TEST,
    AFTER,
    AFTER_CLASS,
  }

  /**
   * Extension that applies at CLASS_RULE place
   */
  public static class ClassRuleExtension implements BeforeAllCallback {
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
      ApplyAtPlace.apply(Place.CLASS_RULE);
    }
  }

  /**
   * Extension that applies at TEST_RULE place
   */
  public static class TestRuleExtension implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
      ApplyAtPlace.apply(Place.TEST_RULE);
    }
  }

  @ExtendWith({RandomizedExtension.class, ClassRuleExtension.class, TestRuleExtension.class})
  public static class ApplyAtPlace extends RandomizedTest {
    public static Place place;
    public static Runnable runnable;

    @BeforeAll 
    public static void beforeClass() { apply(Place.BEFORE_CLASS); }

    public ApplyAtPlace() { apply(Place.CONSTRUCTOR); }

    @BeforeEach 
    public void before() { apply(Place.BEFORE); }

    @Test
    public void testMethod() { apply(Place.TEST); }

    @AfterEach
    public void after() { apply(Place.AFTER); }

    @AfterAll 
    public static void afterClass() { apply(Place.AFTER_CLASS); }

    static void apply(Place p) {
      if (place == p) {
        assumeRunningNested();
        runnable.run();
      }
    }
  }

  @RegisterExtension
  static SystemPropertiesInvariantExtension noLeftOverProperties =
      new SystemPropertiesInvariantExtension(new HashSet<String>(Arrays.asList(
          "user.timezone")));

  /** For capturing sysout. */
  protected static PrintStream sysout;

  /** For capturing syserr. */
  protected static PrintStream syserr;

  /**
   * Captured sysout/ syserr.
   */
  private static StringWriter sw;
  
  /**
   * Captured java logging messages. 
   */
  private static StringWriter loggingMessages;

  /**
   * Main logger.
   */
  private static Logger logger;

  /**
   * Previous handlers..
   */
  private static Handler[] handlers;

  /**
   * Zombie threads.
   */
  private static List<Thread> zombies = new ArrayList<>();

  private static volatile Object zombieToken;

  @BeforeAll
  public static final void setupNested() throws IOException {
    runningNested = true;
    zombieToken = new Object();

    // capture sysout/ syserr.
    sw = new StringWriter();
    sysout = System.out;
    syserr = System.err;
    System.setOut(new PrintStream(new TeeOutputStream(System.out, new WriterOutputStream(sw))));
    System.setErr(new PrintStream(new TeeOutputStream(System.err, new WriterOutputStream(sw))));
    
    // Add custom logging handler because java logging keeps a reference to previous System.err.
    loggingMessages = new StringWriter();
    logger = Logger.getLogger("");
    handlers = logger.getHandlers();
    for (Handler h : handlers) logger.removeHandler(h);
    logger.addHandler(new Handler() {
        final SimpleFormatter formatter = new SimpleFormatter();

        @Override
        public void publish(LogRecord record) {
          loggingMessages.write(formatter.format(record) + "\n");
        }

        @Override
        public void flush() {}
        
        @Override
        public void close() throws SecurityException {}
      });
  }

  @AfterAll
  public static final void clearNested() throws Exception {
    zombieToken = null;
    runningNested = false;
    ApplyAtPlace.runnable = null;
    ApplyAtPlace.place = null;
    
    System.setOut(sysout);
    System.setErr(syserr);

    for (Handler h : logger.getHandlers()) logger.removeHandler(h);
    for (Handler h : handlers) logger.addHandler(h);
    
    for (Thread t : zombies) {
      t.interrupt();
    }
    for (Thread t : zombies) {
      t.join();
    }    
  }

  @AfterEach
  public void afterEach() {
    // Reset zombie thread marker - this was previously using RandomizedRunner.zombieMarker
    // In JUnit 5, thread leak detection is handled differently
  }
  
  @BeforeEach
  public void beforeEach() {
    sw.getBuffer().setLength(0);
    loggingMessages.getBuffer().setLength(0);
  }
  
  protected static String getSysouts() {
    System.out.flush();
    System.err.flush();
    return sw.toString();
  }

  public static String getLoggingMessages() {
    return loggingMessages.toString();
  }
  
  protected static boolean isRunningNested() {
    return runningNested;
  }
  
  protected static void assumeRunningNested() {
    if (!runningNested) {
      throw new TestAbortedException("Not running nested");
    }
  }
  
  protected static Thread startZombieThread(String name) {
    final CountDownLatch latch = new CountDownLatch(1);
    Thread t = new Thread(name) {
      private final Object token = zombieToken; 

      public void run() {
        latch.countDown();
        while (zombieToken == token) {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            // ignore.
          }
        }
      }
    };
    t.start();
    zombies.add(t);
    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return t;
  }
  
  protected static Thread startThread(String name) {
    final CountDownLatch latch = new CountDownLatch(1);
    Thread t = new Thread(name) {
      public void run() {
        latch.countDown();
        sleepForever();
      }

      private void sleepForever() {
        while (true) RandomizedTest.sleep(1000);
      }
    };
    t.start();
    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return t;
  }

  /**
   * Result of running tests with JUnit Platform.
   */
  public static class FullResult {
    private final AtomicInteger runCount = new AtomicInteger();
    private final AtomicInteger ignoreCount = new AtomicInteger();
    private final AtomicInteger failureCount = new AtomicInteger();
    private final AtomicInteger assumptionIgnored = new AtomicInteger();
    private final List<FailureInfo> failures = new ArrayList<>();
    
    public int getRunCount() {
      return runCount.get();
    }

    public int getIgnoreCount() {
      return ignoreCount.get();
    }
    
    public int getFailureCount() {
      return failureCount.get();
    }

    public int getAssumptionIgnored() {
      return assumptionIgnored.get();
    }

    public List<FailureInfo> getFailures() {
      return failures;
    }

    public boolean wasSuccessful() {
      return failureCount.get() == 0;
    }
  }

  /**
   * Represents a test failure.
   */
  public static class FailureInfo {
    private final TestIdentifier testIdentifier;
    private final Throwable exception;

    public FailureInfo(TestIdentifier testIdentifier, Throwable exception) {
      this.testIdentifier = testIdentifier;
      this.exception = exception;
    }

    public TestIdentifier getTestIdentifier() {
      return testIdentifier;
    }

    public Throwable getException() {
      return exception;
    }

    public String getTrace() {
      StringWriter sw = new StringWriter();
      exception.printStackTrace(new java.io.PrintWriter(sw));
      return sw.toString();
    }

    @Override
    public String toString() {
      return testIdentifier.getDisplayName() + ": " + exception.getMessage();
    }
  }
  
  /**
   * Run tests using JUnit Platform Launcher.
   */
  public static FullResult runTests(final Class<?>... classes) {
    try {
      final FullResult fullResult = new FullResult();
      
      // Run on a separate thread so that it appears as we're not running in an IDE. 
      Thread thread = new Thread() {
        @Override
        public void run() {
          LauncherDiscoveryRequestBuilder requestBuilder = LauncherDiscoveryRequestBuilder.request();
          for (Class<?> clazz : classes) {
            requestBuilder.selectors(selectClass(clazz));
          }
          LauncherDiscoveryRequest request = requestBuilder.build();
          
          Launcher launcher = LauncherFactory.create();
          
          // Add our print listener
          launcher.registerTestExecutionListeners(new PrintEventListener(sysout));
          
          // Add result collecting listener
          launcher.registerTestExecutionListeners(new TestExecutionListener() {
            @Override
            public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
              if (testIdentifier.isTest()) {
                fullResult.runCount.incrementAndGet();
                
                if (testExecutionResult.getStatus() == TestExecutionResult.Status.FAILED) {
                  fullResult.failureCount.incrementAndGet();
                  Optional<Throwable> throwable = testExecutionResult.getThrowable();
                  fullResult.failures.add(new FailureInfo(testIdentifier, 
                      throwable.orElse(new RuntimeException("Unknown failure"))));
                } else if (testExecutionResult.getStatus() == TestExecutionResult.Status.ABORTED) {
                  fullResult.assumptionIgnored.incrementAndGet();
                }
              } else if (testIdentifier.isContainer()) {
                // Container-level failures (e.g., extension failures in @AfterAll)
                if (testExecutionResult.getStatus() == TestExecutionResult.Status.FAILED) {
                  fullResult.failureCount.incrementAndGet();
                  Optional<Throwable> throwable = testExecutionResult.getThrowable();
                  fullResult.failures.add(new FailureInfo(testIdentifier, 
                      throwable.orElse(new RuntimeException("Container failure"))));
                } else if (testExecutionResult.getStatus() == TestExecutionResult.Status.ABORTED) {
                  fullResult.assumptionIgnored.incrementAndGet();
                }
              }
            }
            
            @Override
            public void executionSkipped(TestIdentifier testIdentifier, String reason) {
              if (testIdentifier.isTest()) {
                fullResult.ignoreCount.incrementAndGet();
              } else if (testIdentifier.isContainer()) {
                // Container-level skips (e.g., @Disabled on class)
                // Count it as ignored since the whole class is skipped
                fullResult.ignoreCount.incrementAndGet();
              }
            }
          });

          launcher.execute(request);
        }
      };

      thread.start();
      thread.join();
      return fullResult;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static FullResult checkTestsOutput(int run, int ignored, int failures, int assumptions, Class<?> classes) {
    FullResult result = runTests(classes);
    if (result.getRunCount() != run ||
        result.getIgnoreCount() != ignored ||
        result.getFailureCount() != failures ||
        result.getAssumptionIgnored() != assumptions) {
      Assertions.fail("Different result. [run,ign,fail,ass] Expected: "
          + run + "," + ignored + "," + failures + "," + assumptions + 
          ", Actual: " + result.getRunCount() + "," + result.getIgnoreCount() + "," + result.getFailureCount()
          + "," + result.getAssumptionIgnored());
    }
    return result;
  }
}

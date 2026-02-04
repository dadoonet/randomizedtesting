package com.carrotsearch.ant.tasks.junit5.forked;

import java.io.IOException;
import java.util.Optional;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.opentest4j.TestAbortedException;

import com.carrotsearch.ant.tasks.junit5.events.*;
import com.carrotsearch.ant.tasks.junit5.events.mirrors.TestDescriptionMirror;

/**
 * Serialize test execution events using JUnit Platform's TestExecutionListener.
 * This is the JUnit 5 equivalent of the old RunListenerEmitter.
 */
public class TestExecutionEmitter implements TestExecutionListener {
  private final Serializer serializer;
  private TestDescriptionMirror suiteDescription;
  private long testStart;
  private long suiteStart;
  
  /** Track the current test for timing. */
  private TestIdentifier currentTest;

  public TestExecutionEmitter(Serializer serializer) {
    this.serializer = serializer;
  }

  @Override
  public void testPlanExecutionStarted(TestPlan testPlan) {
    // This is called once before all tests
  }

  @Override
  public void testPlanExecutionFinished(TestPlan testPlan) {
    // This is called once after all tests
  }

  @Override
  public void executionStarted(TestIdentifier testIdentifier) {
    try {
      if (testIdentifier.isContainer()) {
        // Suite/class started
        if (isClassContainer(testIdentifier)) {
          suiteDescription = toDescriptionMirror(testIdentifier);
          suiteStart = System.currentTimeMillis();
          serializer.serialize(new SuiteStartedEvent(suiteDescription, suiteStart));
        }
      } else {
        // Test method started
        currentTest = testIdentifier;
        testStart = System.currentTimeMillis();
        serializer.serialize(new TestStartedEvent(toDescriptionMirror(testIdentifier)));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
    try {
      if (testIdentifier.isContainer()) {
        // Suite/class finished
        if (isClassContainer(testIdentifier)) {
          long duration = System.currentTimeMillis() - suiteStart;
          
          // Handle container failures
          if (result.getStatus() == TestExecutionResult.Status.FAILED) {
            result.getThrowable().ifPresent(t -> {
              try {
                serializer.serialize(new SuiteFailureEvent(suiteDescription, t));
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
          }
          
          serializer.serialize(new SuiteCompletedEvent(suiteDescription, suiteStart, duration));
          suiteDescription = null;
        }
      } else {
        // Test method finished
        long executionTime = System.currentTimeMillis() - testStart;
        TestDescriptionMirror testDescription = toDescriptionMirror(testIdentifier);
        
        switch (result.getStatus()) {
          case SUCCESSFUL:
            // Just emit finished event
            break;
          case FAILED:
            result.getThrowable().ifPresent(t -> {
              try {
                serializer.serialize(new TestFailureEvent(testDescription, t));
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
            break;
          case ABORTED:
            // Assumption failure or test aborted
            result.getThrowable().ifPresent(t -> {
              try {
                serializer.serialize(new TestIgnoredAssumptionEvent(testDescription, t));
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
            break;
        }
        
        serializer.serialize(new TestFinishedEvent(testDescription, executionTime, testStart));
        currentTest = null;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void executionSkipped(TestIdentifier testIdentifier, String reason) {
    try {
      if (testIdentifier.isContainer()) {
        // Entire class skipped
        if (isClassContainer(testIdentifier)) {
          suiteDescription = toDescriptionMirror(testIdentifier);
          suiteStart = System.currentTimeMillis();
          serializer.serialize(new SuiteStartedEvent(suiteDescription, suiteStart));
          // Emit ignored event for the suite
          serializer.serialize(new TestIgnoredEvent(suiteDescription, reason != null ? reason : "Skipped"));
          serializer.serialize(new SuiteCompletedEvent(suiteDescription, suiteStart, 0));
          suiteDescription = null;
        }
      } else {
        // Individual test skipped (e.g., @Disabled)
        TestDescriptionMirror testDescription = toDescriptionMirror(testIdentifier);
        serializer.serialize(new TestIgnoredEvent(testDescription, reason != null ? reason : "Skipped"));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void dynamicTestRegistered(TestIdentifier testIdentifier) {
    // Dynamic tests (e.g., @TestFactory) - handled through normal execution events
  }

  /**
   * Check if this is a class-level container (not the engine or package level).
   */
  private boolean isClassContainer(TestIdentifier testIdentifier) {
    Optional<TestSource> source = testIdentifier.getSource();
    return source.isPresent() && source.get() instanceof ClassSource;
  }

  /**
   * Convert a TestIdentifier to our TestDescriptionMirror.
   */
  private TestDescriptionMirror toDescriptionMirror(TestIdentifier testIdentifier) {
    String displayName = testIdentifier.getDisplayName();
    String className = null;
    String methodName = null;
    String uniqueId = testIdentifier.getUniqueId();

    Optional<TestSource> source = testIdentifier.getSource();
    if (source.isPresent()) {
      TestSource testSource = source.get();
      if (testSource instanceof ClassSource) {
        className = ((ClassSource) testSource).getClassName();
      } else if (testSource instanceof MethodSource) {
        MethodSource methodSource = (MethodSource) testSource;
        className = methodSource.getClassName();
        methodName = methodSource.getMethodName();
      }
    }

    return new TestDescriptionMirror(displayName, className, methodName, uniqueId);
  }
}

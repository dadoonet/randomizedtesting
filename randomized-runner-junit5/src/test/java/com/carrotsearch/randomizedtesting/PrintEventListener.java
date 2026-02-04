package com.carrotsearch.randomizedtesting;

import java.io.PrintStream;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * A JUnit 5 Platform listener that prints test events.
 */
public class PrintEventListener implements TestExecutionListener {
  private final PrintStream out;
  private final AtomicInteger runCount = new AtomicInteger();
  private final AtomicInteger ignoreCount = new AtomicInteger();
  private final AtomicInteger failureCount = new AtomicInteger();
  private final AtomicInteger assumptions = new AtomicInteger();

  public PrintEventListener(PrintStream out) {
    this.out = out;
  }
  
  @Override
  public void testPlanExecutionStarted(TestPlan testPlan) {
    out.println("Run started.");
  }

  @Override
  public void testPlanExecutionFinished(TestPlan testPlan) {
    out.println(String.format(Locale.ROOT, 
        "Run finished: run=%s, ignored=%s, failures=%s, assumptions=%s\n", 
        runCount.get(),
        ignoreCount.get(),
        failureCount.get(),
        assumptions.get()));
  }

  @Override
  public void executionStarted(TestIdentifier testIdentifier) {
    if (testIdentifier.isTest()) {
      out.println("Started : " + testIdentifier.getDisplayName());
    }
  }

  @Override
  public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
    if (testIdentifier.isTest()) {
      runCount.incrementAndGet();
      out.println("Finished: " + testIdentifier.getDisplayName());
      
      if (testExecutionResult.getStatus() == TestExecutionResult.Status.FAILED) {
        failureCount.incrementAndGet();
        out.println("Failure : " + testIdentifier.getDisplayName() + " - " + 
            testExecutionResult.getThrowable().map(Throwable::getMessage).orElse(""));
      } else if (testExecutionResult.getStatus() == TestExecutionResult.Status.ABORTED) {
        assumptions.incrementAndGet();
        out.println("Assumpt.: " + testIdentifier.getDisplayName());
      }
    }
  }

  @Override
  public void executionSkipped(TestIdentifier testIdentifier, String reason) {
    if (testIdentifier.isTest()) {
      ignoreCount.incrementAndGet();
      out.println("Ignored : " + testIdentifier.getDisplayName() + (reason != null ? " - " + reason : ""));
    }
  }
  
  public int getRunCount() {
    return runCount.get();
  }
  
  public int getIgnoreCount() {
    return ignoreCount.get();
  }
  
  public int getFailureCount() {
    return failureCount.get();
  }
  
  public int getAssumptionCount() {
    return assumptions.get();
  }
}

package com.carrotsearch.randomizedtesting.listeners;

import org.opentest4j.TestAbortedException;

import com.carrotsearch.randomizedtesting.*;
import com.carrotsearch.randomizedtesting.annotations.SuppressForbidden;

/**
 * A {@link RandomizedTestListener} that emits to {@link System#err} a string with command
 * line parameters allowing quick test re-run under ANT command line.     
 */
public class ReproduceInfoPrinter implements RandomizedTestListener {
  
  @Override
  @SuppressForbidden("Legitimate use of syserr.")
  public void testFailed(TestInfo testInfo, Throwable cause) {
    // Ignore assumptions.
    if (cause instanceof TestAbortedException) {
      return;
    }

    final StringBuilder b = new StringBuilder();
    b.append("FAILURE  : ").append(testInfo.getDisplayName()).append("\n");
    b.append("Message  : ").append(cause != null ? cause.getMessage() : "unknown").append("\n");
    b.append("Reproduce: ");
    
    // Try to get seed information from context
    try {
      RandomizedContext ctx = RandomizedContext.current();
      b.append("-Dtests.seed=").append(ctx.getRunnerSeedAsString());
      if (ctx.getTargetMethod() != null) {
        b.append(" -Dtests.method=").append(ctx.getTargetMethod().getName());
      }
      b.append(" -Dtests.class=").append(ctx.getTargetClass().getName());
    } catch (IllegalStateException e) {
      // No context available
      b.append("(no context available)");
    }

    b.append("\n");
    b.append("Throwable:\n");
    if (cause != null) {
      TraceFormatting traces = new TraceFormatting();
      traces.formatThrowable(b, cause);
    }

    System.err.println(b.toString());
  }
}

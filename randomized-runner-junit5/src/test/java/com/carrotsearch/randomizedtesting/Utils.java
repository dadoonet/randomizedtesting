package com.carrotsearch.randomizedtesting;

import java.util.Set;
import java.util.logging.Logger;

import org.assertj.core.api.Assertions;

import com.carrotsearch.randomizedtesting.WithNestedTestClass.FailureInfo;
import com.carrotsearch.randomizedtesting.WithNestedTestClass.FullResult;

import static org.junit.jupiter.api.Assertions.*;

public class Utils {
  /**
   * Assert a result has at least one failure with message.
   */
  public static void assertFailureWithMessage(FullResult r, String message) {
    for (FailureInfo f : r.getFailures()) {
      if (f.getTrace().contains(message)) {
        return;
      }
    }

    StringBuilder b = new StringBuilder("No failure with message: '" + message + "' (" +
        r.getFailures().size() + " failures):");
    for (FailureInfo f : r.getFailures()) {
      b.append("\n\t- ").append(f.getTrace());
    }
    Logger.getLogger("").severe(b.toString());
    fail(b.toString());
  }

  public static void assertNoFailureWithMessage(FullResult r, String message) {
    boolean hadMessage = false;
    for (FailureInfo f : r.getFailures()) {
      if (f.getTrace().contains(message)) {
        hadMessage = true;
      }
    }
    
    if (!hadMessage) return;

    StringBuilder b = new StringBuilder("Failure with message: '" + message + "' (" +
        r.getFailures().size() + " failures):");
    for (FailureInfo f : r.getFailures()) {
      b.append("\n\t- ").append(f.getTrace());
    }
    Logger.getLogger("").severe(b.toString());
    fail(b.toString());
  }

  /**
   * Check that all thrown failures have been augmented and contain
   * a synthetic seed frame.
   */
  public static void assertFailuresContainSeeds(FullResult r) {
    for (FailureInfo f : r.getFailures()) {
      String seed = RandomizedRunnerConstants.seedFromThrowable(f.getException());
      assertTrue(seed != null, "Not augmented: " + f.getTrace());
    }
  }

  /**
   * Package scope test access to {@link RandomizedContext#getRunnerSeed()}.
   */
  public static long getRunnerSeed() {
    return RandomizedContext.current().getRunnerSeed();
  }

  /**
   * Package scope test access to {@link Randomness#getSeed()}. 
   */
  public static long getSeed(Randomness randomness) {
    return randomness.getSeed();
  }

  /**
   * Assert no threads with the given substring are present.
   */
  public static void assertNoLiveThreadsContaining(String substring) {
    for (Thread t : getAllThreads()) {
      if (t.isAlive()) {
        Assertions.assertThat(t.getName())
          .as("Unexpected live thread").doesNotContain(substring);
      }
    }
  }

  /**
   * Expose to non-package scope. 
   */
  public static Set<Thread> getAllThreads() {
    return Threads.getAllThreads();
  }
  
  /**
   * Expose to non-package scope. 
   */
  public static ThreadGroup getTopThreadGroup() {
    return Threads.getTopThreadGroup();
  }  
}

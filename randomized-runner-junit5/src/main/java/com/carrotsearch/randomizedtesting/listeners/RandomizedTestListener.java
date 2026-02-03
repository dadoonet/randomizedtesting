package com.carrotsearch.randomizedtesting.listeners;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestIdentifier;

/**
 * A listener interface for randomized test events.
 * This is the JUnit 5 equivalent of JUnit 4's RunListener.
 */
public interface RandomizedTestListener {
  
  /**
   * Called before any tests have been run.
   */
  default void testRunStarted() {}

  /**
   * Called when all tests have finished.
   */
  default void testRunFinished() {}

  /**
   * Called when a test is about to be started.
   */
  default void testStarted(TestIdentifier testIdentifier) {}

  /**
   * Called when a test has finished, whether successful or not.
   */
  default void testFinished(TestIdentifier testIdentifier, TestExecutionResult result) {}

  /**
   * Called when a test fails.
   */
  default void testFailed(TestIdentifier testIdentifier, Throwable cause) {}

  /**
   * Called when a test is skipped.
   */
  default void testSkipped(TestIdentifier testIdentifier, String reason) {}

  /**
   * Called when a test is aborted (assumption failure).
   */
  default void testAborted(TestIdentifier testIdentifier, Throwable cause) {}
}

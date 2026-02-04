package com.carrotsearch.randomizedtesting.listeners;

/**
 * A listener interface for randomized test events.
 * This is the JUnit 5 equivalent of JUnit 4's RunListener.
 */
public interface RandomizedTestListener {
  
  /**
   * Called before any tests have been run (at suite level).
   */
  default void testRunStarted() {}

  /**
   * Called when all tests have finished (at suite level).
   * @param runCount the number of tests that were run
   */
  default void testRunFinished(int runCount) {}

  /**
   * Called when a test is about to be started.
   */
  default void testStarted(TestInfo testInfo) {}

  /**
   * Called when a test has finished successfully.
   */
  default void testFinished(TestInfo testInfo) {}

  /**
   * Called when a test fails.
   */
  default void testFailed(TestInfo testInfo, Throwable cause) {}

  /**
   * Called when a test is skipped (e.g., @Disabled).
   */
  default void testSkipped(TestInfo testInfo, String reason) {}

  /**
   * Called when a test is aborted (assumption failure).
   */
  default void testAborted(TestInfo testInfo, Throwable cause) {}
}

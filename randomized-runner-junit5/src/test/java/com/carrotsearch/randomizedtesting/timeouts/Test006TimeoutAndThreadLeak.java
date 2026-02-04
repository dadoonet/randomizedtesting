package com.carrotsearch.randomizedtesting.timeouts;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.Utils;
import com.carrotsearch.randomizedtesting.WithNestedTestClass;
import com.carrotsearch.randomizedtesting.annotations.TimeoutSuite;

/**
 * Tests for suite timeout with thread leak cleanup.
 * 
 * Note: In JUnit 5, we can only intercept actual lifecycle methods (@BeforeAll, @AfterAll,
 * @BeforeEach, @AfterEach) and test methods. Extension callbacks (like BeforeAllCallback)
 * and constructors cannot be intercepted, so those tests are not applicable.
 */
public class Test006TimeoutAndThreadLeak extends WithNestedTestClass {
  /**
   * Nested test suite class with {@link TimeoutSuite}.
   */
  @TimeoutSuite(millis = 500)
  public static class Nested extends ApplyAtPlace {}

  // Note: testClassRule, testConstructor, and testTestRule are not applicable in JUnit 5
  // because InvocationInterceptor cannot intercept extension callbacks or constructors.

  @Test public void testBeforeClass() { check(Place.BEFORE_CLASS); }
  @Test public void testBefore() { check(Place.BEFORE); }
  @Test public void testTest() { check(Place.TEST); }
  @Test public void testAfter() { check(Place.AFTER); }
  @Test public void testAfterClass() { check(Place.AFTER_CLASS); }

  /**
   * Check a given timeout place. 
   */
  private void check(Place p) {
    ApplyAtPlace.place = p;
    ApplyAtPlace.runnable = new Runnable() {
      @Override
      public void run() {
        startThread("foobar");
        while (true) RandomizedTest.sleep(1000);
      }
    };

    FullResult r = runTests(Nested.class);
    Utils.assertFailureWithMessage(r, "Suite timeout exceeded");
    Utils.assertFailuresContainSeeds(r);
    Utils.assertNoLiveThreadsContaining("foobar");

    Assertions.assertThat(getLoggingMessages())
      .doesNotContain("Uncaught exception");
  }
}

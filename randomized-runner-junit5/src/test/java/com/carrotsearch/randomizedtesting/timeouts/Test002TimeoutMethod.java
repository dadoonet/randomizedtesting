package com.carrotsearch.randomizedtesting.timeouts;

import org.junit.jupiter.api.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.Utils;
import com.carrotsearch.randomizedtesting.WithNestedTestClass;
import com.carrotsearch.randomizedtesting.annotations.Timeout;

/**
 * Tests for method timeout on lifecycle methods.
 * 
 * Note: In JUnit 5, we can only intercept actual lifecycle methods (@BeforeEach, @AfterEach)
 * and test methods. Extension callbacks (like BeforeEachCallback) cannot be intercepted,
 * so testTestRule is not applicable.
 */
public class Test002TimeoutMethod extends WithNestedTestClass {
  @Timeout(millis = 25)
  public static class Nested extends ApplyAtPlace {}

  // Note: testTestRule is not applicable in JUnit 5 because InvocationInterceptor
  // cannot intercept extension callbacks.
  
  @Test public void testBefore() { check(Place.BEFORE); }
  @Test public void testTest() { check(Place.TEST); }
  @Test public void testAfter() { check(Place.AFTER); }

  /**
   * Check a given timeout place. 
   */
  private void check(Place p) {
    ApplyAtPlace.place = p;
    ApplyAtPlace.runnable = new Runnable() {
      @Override
      public void run() {
        while (true) RandomizedTest.sleep(1000);
      }
    };

    FullResult r = runTests(Nested.class);
    Utils.assertFailureWithMessage(r, "Test timeout exceeded");
    Utils.assertFailuresContainSeeds(r);
  }
}

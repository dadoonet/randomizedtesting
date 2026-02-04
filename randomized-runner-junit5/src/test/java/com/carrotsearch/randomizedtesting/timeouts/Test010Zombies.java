package com.carrotsearch.randomizedtesting.timeouts;

import java.util.Arrays;
import java.util.HashSet;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.SysGlobals;
import com.carrotsearch.randomizedtesting.ThreadLeakControl;
import com.carrotsearch.randomizedtesting.Utils;
import com.carrotsearch.randomizedtesting.WithNestedTestClass;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakAction;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakAction.Action;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakLingering;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope.Scope;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakZombies;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakZombies.Consequence;
import com.carrotsearch.randomizedtesting.extensions.SystemPropertiesInvariantExtension;

/**
 * Tests for zombie thread detection and handling.
 * 
 * Note: In JUnit 5, we can only intercept lifecycle methods (@BeforeAll, @AfterAll, 
 * @BeforeEach, @AfterEach) and test methods. Extension callbacks and constructors
 * cannot be intercepted.
 */
public class Test010Zombies extends WithNestedTestClass {
  
  @ThreadLeakScope(Scope.TEST)
  @ThreadLeakLingering(linger = 0)
  @ThreadLeakAction({Action.INTERRUPT})
  @ThreadLeakZombies(Consequence.IGNORE_REMAINING_TESTS)
  public static class Nested extends ApplyAtPlace {}

  @RegisterExtension
  static SystemPropertiesInvariantExtension restoreProperties = 
      new SystemPropertiesInvariantExtension(new HashSet<>(Arrays.asList(
          SysGlobals.SYSPROP_KILLWAIT(),
          SysGlobals.SYSPROP_KILLATTEMPTS(),
          "user.timezone")));

  @BeforeEach
  public void setup() {
    // Reset zombie marker before each test
    ThreadLeakControl.resetZombieMarker();
  }

  @AfterEach
  public void cleanup() {
    // Clean up system properties
    System.clearProperty(SysGlobals.SYSPROP_KILLWAIT());
    System.clearProperty(SysGlobals.SYSPROP_KILLATTEMPTS());
    // Reset zombie marker after test
    ThreadLeakControl.resetZombieMarker();
  }

  // Note: testClassRule, testConstructor, and testTestRule are not applicable in JUnit 5
  // because InvocationInterceptor cannot intercept extension callbacks or constructors.

  @Test 
  public void testBeforeClass() { 
    check(Place.BEFORE_CLASS); 
  }

  @Test 
  public void testBefore() { 
    check(Place.BEFORE); 
  }

  @Test 
  public void testTest() { 
    check(Place.TEST); 
  }

  @Test 
  public void testAfter() { 
    check(Place.AFTER); 
  }

  @Test 
  public void testAfterClass() { 
    check(Place.AFTER_CLASS); 
  }

  /**
   * Start a zombie thread somewhere. Ensure all suites are ignored afterwards.
   */
  private void check(Place p) {
    System.setProperty(SysGlobals.SYSPROP_KILLWAIT(), "10");
    System.setProperty(SysGlobals.SYSPROP_KILLATTEMPTS(), "2");

    ApplyAtPlace.place = p;
    ApplyAtPlace.runnable = new Runnable() {
      @Override
      public void run() {
        startZombieThread("foobarZombie");
      }
    };

    // Run a class spawning zombie threads.
    FullResult r = runTests(Nested.class);

    Utils.assertFailureWithMessage(r, "1 thread leaked");
    Utils.assertFailureWithMessage(r, "foobarZombie");
    Utils.assertFailuresContainSeeds(r);

    Assertions.assertThat(getLoggingMessages())
      .contains("There are still zombie threads that couldn't be terminated:");

    // Run another suite. Everything should be ignored because of zombie threads.
    for (Place p2 : Place.values()) {
      // Skip places that aren't interceptable in JUnit 5
      if (p2 == Place.CLASS_RULE || p2 == Place.CONSTRUCTOR || p2 == Place.TEST_RULE) {
        continue;
      }
      
      ApplyAtPlace.place = p2;
      ApplyAtPlace.runnable = new Runnable() {
        @Override
        public void run() {
          throw new RuntimeException("This should not be executed");
        }
      };

      r = runTests(Nested.class);
      // Tests should be skipped due to zombies (runCount should be 0 or all passed)
      Assertions.assertThat(r.getFailureCount()).as("At: " + p2).isZero();
    }
  }
}

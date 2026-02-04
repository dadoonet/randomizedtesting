package com.carrotsearch.randomizedtesting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Random;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope.Scope;

/**
 * Check out of scope {@link Random} use.
 * 
 * Note: The @Timeout scenario from JUnit 4 is not applicable in JUnit 5 because
 * timeout execution runs in a different thread than @BeforeEach hooks due to
 * architectural differences between JUnit 4's RandomizedRunner and JUnit 5's extension model.
 */
public class TestOutOfScopeRandomUse extends WithNestedTestClass {

  @ExtendWith(RandomizedExtension.class)
  @ThreadLeakScope(Scope.NONE)
  public static class Nested extends RandomizedTest {
    static Random instanceRandom;
    static Random beforeHookRandom;
    static Random staticContextRandom;
    volatile static Random otherThreadRandom;

    @BeforeAll
    public static void beforeClass() throws Exception {
      assumeRunningNested();
      instanceRandom = null;
      staticContextRandom = getRandom();
      
      // Should be able to use the random we've acquired for the static context.
      staticContextRandom.nextBoolean();
      
      Thread t = new Thread() {
        public void run() {
          otherThreadRandom = getRandom();
        }
      };
      t.start();
      t.join();
    }
    
    @AfterAll
    public static void afterClass() {
      if (!isRunningNested()) {
        return;
      }
      
      // Again should be able to use the random we've acquired for the static context.
      staticContextRandom.nextBoolean();
    }

    @BeforeEach
    public void before() {
      beforeHookRandom = getRandom();
    }

    private void touchRandom() {
      assumeRunningNested();

      // We shouldn't be able to reach to some other thread's random for which
      // the context is still valid.
      try {
        otherThreadRandom.nextBoolean();
        fail("Shouldn't be able to use another thread's Random.");
      } catch (IllegalStateException e) {
        // Expected.
      }

      // We should always be able to reach to @BeforeEach hook initialized Random.
      beforeHookRandom.nextBoolean();
      
      // Check if we're the first method or the latter methods.
      if (instanceRandom == null) {
        instanceRandom = getRandom();
      } else {
        // for anything not-first, we shouldn't be able to reuse first random anymore.
        try {
          instanceRandom.nextBoolean();
          fail("Shouldn't be able to use another test's Random.");
        } catch (IllegalStateException e) {
          // Expected.
        }
      }
    }

    @Test
    public void method1() throws Exception {
      touchRandom();
    }

    @Test
    public void method2() throws Exception {
      touchRandom();
    }    
  }

  @BeforeEach
  public void checkRunningWithAssertions() {
    // Sharing Random is only checked with -ea
    // https://github.com/randomizedtesting/randomizedtesting/issues/234
    assumeTrue(AssertingRandom.isVerifying(), "AssertionRandom not verifying sharing.");
  }
  
  @Test
  public void testCrossTestCaseIsolation() throws Throwable {
    assertThat(runTests(Nested.class).getFailures()).isEmpty();
  }

  @Test
  public void testCrossTestSuiteIsolation() {
    runTests(Nested.class);
    try {
      Nested.staticContextRandom.nextBoolean();
      fail("Shouldn't be able to use another suite's Random.");
    } catch (IllegalStateException e) {
      // Expected.
    }
  }
}

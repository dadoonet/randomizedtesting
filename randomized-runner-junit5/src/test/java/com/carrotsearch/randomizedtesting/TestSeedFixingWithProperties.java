package com.carrotsearch.randomizedtesting;

import static com.carrotsearch.randomizedtesting.SysGlobals.SYSPROP_ITERATIONS;
import static com.carrotsearch.randomizedtesting.SysGlobals.SYSPROP_RANDOM_SEED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Seed fixing for static fixtures and/or methods using system properties.
 */
public class TestSeedFixingWithProperties extends WithNestedTestClass {
  static List<Long> seeds = new ArrayList<Long>();

  @ExtendWith(RandomizedExtension.class)
  public static class Nested {
    @BeforeAll
    public static void staticFixture() {
      seeds.add(RandomizedContext.current().getRandomness().getSeed());
    }

    @Test
    public void testMethod() {
      seeds.add(RandomizedContext.current().getRandomness().getSeed());
    }
  }

  /**
   * Combined seed: fixing everything: the runner, method and any repetitions.
   */
  @Test
  public void testRunnerAndMethodProperty() {
    System.setProperty(SYSPROP_RANDOM_SEED(), "deadbeef:cafebabe");
    System.setProperty(SYSPROP_ITERATIONS(), "3");
    runTests(Nested.class);
    assertEquals(Arrays.asList(0xdeadbeefL, 0xcafebabeL, 0xcafebabeL, 0xcafebabeL), seeds);
  }

  /**
   * Runner seed fixing only (methods have predictable pseudo-random seeds derived from 
   * the runner). 
   * 
   * Note: In JUnit 5, iterations happen within a single test method invocation,
   * so only 1 test run is reported even though the method executes 3 times.
   */
  @Test
  public void testFixedRunnerPropertyOnly() {
    System.setProperty(SYSPROP_RANDOM_SEED(), "deadbeef");
    System.setProperty(SYSPROP_ITERATIONS(), "3");
    // JUnit 5 reports 1 run even though the method executes 3 times internally
    checkTestsOutput(1, 0, 0, 0, Nested.class);
    // We should have 4 seeds: 1 from @BeforeAll + 3 from testMethod iterations
    assertEquals(4, seeds.size(), "Expected 4 seeds (1 @BeforeAll + 3 iterations)");
    assertEquals(0xdeadbeefL, seeds.get(0).longValue());
    // _very_ slim chances of this actually being true...
    assertFalse(
        seeds.get(0).longValue() == seeds.get(1).longValue() &&
        seeds.get(1).longValue() == seeds.get(2).longValue());

    // We should have the same randomized seeds on methods for the same runner seed,
    // so check if this is indeed true.
    List<Long> copy = new ArrayList<Long>(seeds);
    seeds.clear();
    runTests(Nested.class);
    assertEquals(copy, seeds);
  }

  @BeforeEach
  public void cleanupBefore() {
    cleanupAfter();
  }
  
  @AfterEach
  public void cleanupAfter() {
    System.clearProperty(SYSPROP_ITERATIONS());
    System.clearProperty(SYSPROP_RANDOM_SEED());
    seeds.clear();
  }
}

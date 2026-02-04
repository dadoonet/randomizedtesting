package com.carrotsearch.randomizedtesting;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Hooks should _not_ execute if there are no test cases to run. Note that
 * ignored test cases behave as if there was something to execute (!).
 */
public class TestHooksWithEmptyTestSet extends WithNestedTestClass {
  @ExtendWith(RandomizedExtension.class)
  public static class Nested {
    static boolean beforeClassExecuted;
    static boolean afterClassExecuted;

    @BeforeAll
    public static void beforeClass() {
      assumeRunningNested();
      beforeClassExecuted = true;
    }

    @AfterAll
    public static void afterClass() {
      afterClassExecuted = true;
    }    
  }
  
  /**
   * Check if methods get the same seed on every run with a fixed runner's seed.
   */
  @Test
  public void testSameMethodRandomnessWithFixedRunner() {
    Nested.beforeClassExecuted = false;
    Nested.afterClassExecuted = false;
    checkTestsOutput(0, 0, 0, 0, Nested.class);
    assertFalse(Nested.beforeClassExecuted);
    assertFalse(Nested.afterClassExecuted);
  }
}

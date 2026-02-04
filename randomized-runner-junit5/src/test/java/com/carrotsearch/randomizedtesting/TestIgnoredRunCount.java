package com.carrotsearch.randomizedtesting;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test run count for ignored/disabled tests.
 */
public class TestIgnoredRunCount extends WithNestedTestClass {
  
  @ExtendWith(RandomizedExtension.class)
  public static class Nested1 {
    @Test @Disabled
    public void ignored() {}
  }

  @ExtendWith(RandomizedExtension.class)
  public static class Nested2 {
    @Test @Disabled
    public void ignored() {}
  }

  @Test
  public void checkIgnoredCount() throws Exception {
    checkTestsOutput(0, 1, 0, 0, Nested1.class);
    checkTestsOutput(0, 1, 0, 0, Nested2.class);
  }
}

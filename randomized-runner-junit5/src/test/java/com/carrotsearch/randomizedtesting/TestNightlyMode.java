package com.carrotsearch.randomizedtesting;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.carrotsearch.randomizedtesting.annotations.Nightly;
import com.carrotsearch.randomizedtesting.annotations.TestGroup;

/**
 * Nightly mode checks.
 */
public class TestNightlyMode extends WithNestedTestClass {
  public static class Nested extends RandomizedTest {
    @Test
    public void passOnNightlyOnly() {
      assumeRunningNested();
      assertTrue(isNightly());
    }

    @Test @Nightly("Nightly only test case.")
    public void nightlyOnly() throws Exception {
    }
  }

  @Test
  public void invalidValueNightly() {
    System.setProperty(TestGroup.Utilities.getSysProperty(Nightly.class), "invalid-value");
    dailyDefault();
  }

  @Test
  public void nightly() {
    System.setProperty(TestGroup.Utilities.getSysProperty(Nightly.class), "yes");
    checkTestsOutput(2, 0, 0, 0, Nested.class);
  }

  @Test
  public void dailyDefault() {
    // In JUnit 5, tests disabled by ExecutionCondition are counted as ignored, not assumption
    checkTestsOutput(1, 1, 1, 0, Nested.class);
  }

  @BeforeEach
  public void cleanupBefore() {
    cleanupAfter();
  }
  
  @AfterEach
  public void cleanupAfter() {
    System.clearProperty(TestGroup.Utilities.getSysProperty(Nightly.class));
  }  
}

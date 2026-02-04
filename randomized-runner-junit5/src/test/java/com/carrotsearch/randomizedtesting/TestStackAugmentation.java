package com.carrotsearch.randomizedtesting;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.annotations.Seed;

/**
 * RandomizedExtension can augment stack traces to include seed info. Check if it works.
 */
public class TestStackAugmentation extends WithNestedTestClass {
  @ExtendWith(RandomizedExtension.class)
  @Seed("deadbeef")
  public static class Nested {
    @Test @Seed("cafebabe")
    public void testMethod1() {
      assumeRunningNested();

      // Throws a chained exception.
      try {
        throw new RuntimeException("Inner.");
      } catch (Exception e) {
        throw new Error("Outer.", e);
      }
    }
  }

  @Test
  public void testMethodLevel() {
    FullResult result = checkTestsOutput(1, 0, 1, 0, Nested.class);

    FailureInfo f = result.getFailures().get(0);
    String seedFromThrowable = RandomizedRunnerConstants.seedFromThrowable(f.getException());
    assertNotNull(seedFromThrowable);
    assertTrue("[DEADBEEF:CAFEBABE]".compareToIgnoreCase(seedFromThrowable) == 0,
        "Expected seed in trace, got: " + seedFromThrowable);
  }

  @ExtendWith(RandomizedExtension.class)
  @Seed("deadbeef")
  public static class Nested2 {
    @BeforeAll
    public static void beforeClass() {
      assumeRunningNested();
      throw new Error("beforeclass.");
    }

    @Test @Seed("cafebabe")
    public void testMethod1() {
    }
  }

  @Test
  public void testBeforeClass() {
    FullResult result = checkTestsOutput(0, 0, 1, 0, Nested2.class);
    assertEquals(1, result.getFailureCount());

    FailureInfo f = result.getFailures().get(0);
    String seedFromThrowable = RandomizedRunnerConstants.seedFromThrowable(f.getException());
    assertNotNull(seedFromThrowable);
    assertTrue("[DEADBEEF]".compareToIgnoreCase(seedFromThrowable) == 0,
        "Expected seed in trace: " + f.getTrace());
  }

  @ExtendWith(RandomizedExtension.class)
  @Seed("deadbeef")
  public static class Nested3 {
    @AfterAll
    public static void afterClass() {
      assumeRunningNested();
      throw new Error("afterclass.");
    }

    @Test @Seed("cafebabe")
    public void testMethod1() {
    }
  }

  @Test
  public void testAfterClass() {
    FullResult result = checkTestsOutput(1, 0, 1, 0, Nested3.class);

    FailureInfo f = result.getFailures().get(0);
    String seedFromThrowable = RandomizedRunnerConstants.seedFromThrowable(f.getException());
    assertNotNull(seedFromThrowable);
    assertTrue("[DEADBEEF]".compareToIgnoreCase(seedFromThrowable) == 0,
        "Expected seed in trace: " + f.getTrace());
  }  
}

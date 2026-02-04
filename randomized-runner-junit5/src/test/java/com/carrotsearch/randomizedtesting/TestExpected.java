package com.carrotsearch.randomizedtesting;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test exception expectations using assertThrows (JUnit 5 equivalent of @Test(expected=...)).
 */
public class TestExpected extends WithNestedTestClass {
  
  /**
   * Nested test class that demonstrates proper exception handling.
   */
  public static class Nested1 extends RandomizedTest {
    @Test
    public void testMethod1() {
      // Using assertThrows to expect RuntimeException
      assertThrows(RuntimeException.class, () -> {
          throw new RuntimeException();
      });
    }
    
    @Test
    public void testMethod2() {
      assumeRunningNested();
      // We expect a RuntimeException but get an Error: should fail
        assertThrows(RuntimeException.class, () -> {
        throw new Error();
      });
    }
  }

  /**
   * Nested test class where no exception is thrown but one is expected.
   */
  public static class Nested2 extends RandomizedTest {
    @Test
    public void testMethod1() {
      assumeRunningNested();
      // Expect RuntimeException but nothing is thrown: should fail
        assertThrows(RuntimeException.class, () -> {
        // Don't do anything - no exception thrown
      });
    }
  }

  @Test
  public void testExpectedFailureDifferentException() {
    // Nested1 has 2 tests:
    // - testMethod1: passes (RuntimeException thrown and expected)
    // - testMethod2: fails (Error thrown but RuntimeException expected)
    FullResult f = checkTestsOutput(2, 0, 1, 0, Nested1.class);
    Assertions.assertThat(f.getFailures().get(0).getException())
      .isInstanceOf(AssertionError.class);
  }

  @Test
  public void testExpectedFailurePassed() {
    // Nested2 has 1 test:
    // - testMethod1: fails (no exception thrown but RuntimeException expected)
    checkTestsOutput(1, 0, 1, 0, Nested2.class);
  }
}

package com.carrotsearch.randomizedtesting.timeouts;

import org.junit.jupiter.api.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.WithNestedTestClass;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope.Scope;
import com.carrotsearch.randomizedtesting.annotations.Timeout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test {@link Timeout} annotation.
 */
public class Test014Timeout extends WithNestedTestClass {
  // Disable thread leak detection as timeout worker threads may not terminate immediately
  @ThreadLeakScope(Scope.NONE)
  public static class Nested extends RandomizedTest {
    @Test
    @Timeout(millis = 100)
    public void testMethod1() {
      assumeRunningNested();
      sleep(2000);
    }

    @Test
    @Timeout(millis = 100)
    public void testMethod2() {
      assumeRunningNested();
      while (!Thread.interrupted()) {
        // Do nothing.
      }
    }
  }

  @Test
  public void testTimeoutInTestAnnotation() {
    FullResult result = runTests(Nested.class);

    assertEquals(0, result.getIgnoreCount());
    assertEquals(2, result.getRunCount());
    assertEquals(2, result.getFailureCount());
    assertEquals(0, result.getAssumptionIgnored());
  }
}

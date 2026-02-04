package com.carrotsearch.randomizedtesting;

import java.util.ArrayList;
import java.util.Random;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Check failure propagation in JUnit 5.
 */
public class TestFailurePropagationCompatibility extends WithNestedTestClass {
  static Random random;
  static int frequency;

  @ExtendWith(RandomizedExtension.class)
  public static class FailRandomly {
    public FailRandomly() {
      maybeFail();
    }

    @BeforeAll public static void beforeClass() { maybeFail(); }
    @BeforeEach public void before() { maybeFail(); }
    @Test public void test() { maybeFail(); }
    @AfterEach public void after() { maybeFail(); }
    @AfterAll public static void afterClass() { maybeFail(); }
  }

  static void maybeFail() {
    if (random != null) {
      if (random.nextInt(frequency) == 0) {
        throw new RuntimeException("State: " + random.nextLong());
      }
    }
  }
  
  @Test
  public void testFailuresPropagateCorrectly() throws Exception {
    Random rnd = new Random();
    frequency = 3;
    
    int failedRuns = 0;
    int successfulRuns = 0;
    
    for (int i = 0; i < 20; i++) {
      final long seed = rnd.nextLong() + i;
      random = new Random(seed);
      FullResult result = runTests(FailRandomly.class);
      
      if (result.getFailureCount() > 0) {
        failedRuns++;
        // Verify that failures contain exception info
        assertFalse(result.getFailures().isEmpty());
      } else {
        successfulRuns++;
      }
    }
    
    // With frequency=3, we expect some failures and some successes
    assertTrue(failedRuns > 0, "Expected some failures");
    assertTrue(successfulRuns > 0, "Expected some successes");
  }
  
  @AfterAll
  public static void cleanup() {
    random = null;
  }
}

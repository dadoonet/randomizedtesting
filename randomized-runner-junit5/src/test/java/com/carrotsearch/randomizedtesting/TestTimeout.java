package com.carrotsearch.randomizedtesting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.annotations.Timeout;
import com.carrotsearch.randomizedtesting.annotations.TimeoutSuite;
import com.carrotsearch.randomizedtesting.extensions.TimeoutExtension;

/**
 * Test for {@link Timeout} and {@link TimeoutSuite} annotation support.
 * 
 * <p>Timeout is now built into {@link RandomizedExtension}, so no separate
 * {@link TimeoutExtension} is needed.
 */
public class TestTimeout extends WithNestedTestClass {
  
  /**
   * Nested test class with method timeout that passes.
   * Uses only RandomizedExtension (timeout is built-in).
   */
  @ExtendWith(RandomizedExtension.class)
  @Timeout(millis = 5000)
  public static class NestedPassingTimeout extends RandomizedTest {
    @Test
    public void fastTest() {
      assumeRunningNested();
      // This should complete well before the timeout
    }
  }
  
  /**
   * Nested test class with method timeout that fails.
   * Uses only RandomizedExtension (timeout is built-in).
   */
  @ExtendWith(RandomizedExtension.class)
  public static class NestedFailingTimeout extends RandomizedTest {
    @Test
    @Timeout(millis = 50)
    public void slowTest() {
      assumeRunningNested();
      // This should timeout
      while (true) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          // Ignore and continue - simulating a test that doesn't respond to interrupts well
        }
      }
    }
  }
  
  @Test
  public void testPassingTimeout() {
    FullResult result = runTests(NestedPassingTimeout.class);
    assertThat(result.getRunCount()).isEqualTo(1);
    assertThat(result.getFailureCount()).isEqualTo(0);
  }
  
  @Test
  public void testFailingTimeout() {
    FullResult result = runTests(NestedFailingTimeout.class);
    assertThat(result.getRunCount()).isEqualTo(1);
    assertThat(result.getFailureCount()).isEqualTo(1);
    assertThat(result.getFailures().get(0).getException().getMessage())
        .contains("timeout");
  }
  
  /**
   * Nested test class with suite timeout that passes.
   */
  @ExtendWith(RandomizedExtension.class)
  @TimeoutSuite(millis = 5000)
  public static class NestedPassingSuiteTimeout extends RandomizedTest {
    @Test
    public void test1() {
      assumeRunningNested();
    }
    
    @Test
    public void test2() {
      assumeRunningNested();
    }
  }
  
  /**
   * Nested test class with suite timeout that fails.
   * The suite timeout is very short and should be exceeded.
   */
  @ExtendWith(RandomizedExtension.class)
  @TimeoutSuite(millis = 25)
  public static class NestedFailingSuiteTimeout extends RandomizedTest {
    @Test
    public void test1() {
      assumeRunningNested();
      // First test should pass (suite just started)
    }
    
    @Test
    public void test2() throws InterruptedException {
      assumeRunningNested();
      // Sleep to exceed suite timeout
      Thread.sleep(50);
    }
    
    @Test
    public void test3() {
      assumeRunningNested();
      // This test should fail because suite timeout was exceeded
    }
  }
  
  @Test
  public void testPassingSuiteTimeout() {
    FullResult result = runTests(NestedPassingSuiteTimeout.class);
    assertThat(result.getRunCount()).isEqualTo(2);
    assertThat(result.getFailureCount()).isEqualTo(0);
  }
  
  @Test
  public void testFailingSuiteTimeout() {
    FullResult result = runTests(NestedFailingSuiteTimeout.class);
    // Some tests should run, but at least one should fail due to suite timeout
    assertThat(result.getFailureCount()).isGreaterThanOrEqualTo(1);
    // Check that we got a suite timeout message
    boolean hasSuiteTimeout = result.getFailures().stream()
        .anyMatch(f -> f.getException().getMessage().contains("Suite timeout exceeded"));
    assertThat(hasSuiteTimeout).isTrue();
  }
}

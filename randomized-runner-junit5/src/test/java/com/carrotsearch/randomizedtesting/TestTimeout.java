package com.carrotsearch.randomizedtesting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.annotations.Timeout;
import com.carrotsearch.randomizedtesting.extensions.TimeoutExtension;

/**
 * Test for {@link Timeout} annotation support.
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
}

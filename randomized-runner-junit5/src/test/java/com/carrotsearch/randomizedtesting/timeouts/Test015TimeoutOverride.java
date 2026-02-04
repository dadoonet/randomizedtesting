package com.carrotsearch.randomizedtesting.timeouts;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.SysGlobals;
import com.carrotsearch.randomizedtesting.WithNestedTestClass;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope.Scope;
import com.carrotsearch.randomizedtesting.annotations.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Test global timeout override (-Dtests.timeout=1000!).
 */
public class Test015TimeoutOverride extends WithNestedTestClass {
  // Disable thread leak detection as timeout worker threads may not terminate immediately
  @ThreadLeakScope(Scope.NONE)
  public static class Nested extends RandomizedTest {
    @Test
    @Timeout(millis = 5000)
    public void testMethod1() {
      assumeRunningNested();
      sleep(10000);
    }
  }

  // Disable thread leak detection for tests with timeout
  @ThreadLeakScope(Scope.NONE)
  public static class Nested2 extends RandomizedTest {
    @Test
    @Timeout(millis = 100)
    public void testMethod1() {
      assumeRunningNested();
      sleep(1000);
    }
  }

  @Test
  public void testTimeoutOverride() {
    System.setProperty(SysGlobals.SYSPROP_TIMEOUT(), "200!");
    long start = System.nanoTime();
    FullResult result = runTests(Nested.class);
    long end = System.nanoTime();
    assertEquals(1, result.getFailureCount());
    assertTrue(TimeUnit.NANOSECONDS.toMillis(end - start) < 3000);
  }
  
  @Test
  public void testDisableTimeout() {
    System.setProperty(SysGlobals.SYSPROP_TIMEOUT(), "0!");

    long start = System.nanoTime();
    FullResult result = runTests(Nested2.class);
    long end = System.nanoTime();
    assertEquals(0, result.getFailureCount());
    assertTrue(TimeUnit.NANOSECONDS.toMillis(end - start) > 900);
  }
  
  @AfterEach
  public void cleanup() {
    System.clearProperty(SysGlobals.SYSPROP_TIMEOUT());
  }
}

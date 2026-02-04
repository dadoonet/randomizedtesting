package com.carrotsearch.randomizedtesting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakAction;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakAction.Action;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakLingering;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope.Scope;

/**
 * Test for thread leak detection.
 */
public class TestThreadLeaks extends WithNestedTestClass {
  
  /**
   * Nested test class that leaks a thread at test level.
   */
  @ExtendWith(RandomizedExtension.class)
  @ThreadLeakScope(Scope.TEST)
  @ThreadLeakLingering(linger = 0)
  @ThreadLeakAction({Action.WARN, Action.INTERRUPT})
  public static class NestedWithTestLeak extends RandomizedTest {
    @Test
    public void leakingTest() throws InterruptedException {
      assumeRunningNested();
      // Start a thread that will leak
      Thread t = new Thread(() -> {
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
          // Expected when interrupted
        }
      }, "leaked-thread-test");
      t.setDaemon(true);
      t.start();
    }
  }
  
  /**
   * Nested test class that leaks a thread at suite level.
   */
  @ExtendWith(RandomizedExtension.class)
  @ThreadLeakScope(Scope.SUITE)
  @ThreadLeakLingering(linger = 0)
  @ThreadLeakAction({Action.WARN, Action.INTERRUPT})
  public static class NestedWithSuiteLeak extends RandomizedTest {
    private static Thread leakedThread;
    
    @Test
    public void test1() {
      assumeRunningNested();
      // Start a thread that will be detected at suite level
      leakedThread = new Thread(() -> {
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
          // Expected when interrupted
        }
      }, "leaked-thread-suite");
      leakedThread.setDaemon(true);
      leakedThread.start();
    }
    
    @Test
    public void test2() {
      assumeRunningNested();
      // Second test - the thread is still running
    }
  }
  
  /**
   * Nested test class without leaks.
   */
  @ExtendWith(RandomizedExtension.class)
  @ThreadLeakScope(Scope.TEST)
  @ThreadLeakLingering(linger = 0)
  @ThreadLeakAction({Action.WARN, Action.INTERRUPT})
  public static class NestedWithoutLeak extends RandomizedTest {
    @Test
    public void cleanTest() {
      assumeRunningNested();
      // This test doesn't leak any threads
    }
  }
  
  /**
   * Nested test with leak scope NONE (disabled).
   */
  @ExtendWith(RandomizedExtension.class)
  @ThreadLeakScope(Scope.NONE)
  public static class NestedWithLeakScopeNone extends RandomizedTest {
    @Test
    public void leakingTestIgnored() throws InterruptedException {
      assumeRunningNested();
      // Start a thread - but leak detection is disabled
      Thread t = new Thread(() -> {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          // Expected
        }
      }, "leaked-but-ignored");
      t.setDaemon(true);
      t.start();
      // Give it time to start
      Thread.sleep(10);
    }
  }
  
  @Test
  public void testNoLeaks() {
    FullResult result = runTests(NestedWithoutLeak.class);
    assertThat(result.getRunCount()).isEqualTo(1);
    assertThat(result.getFailureCount()).isEqualTo(0);
  }
  
  @Test
  public void testTestLevelLeak() {
    FullResult result = runTests(NestedWithTestLeak.class);
    assertThat(result.getRunCount()).isEqualTo(1);
    assertThat(result.getFailureCount()).isEqualTo(1);
    assertThat(result.getFailures().get(0).getException().getMessage())
        .contains("thread leaked from TEST scope");
  }
  
  @Test
  public void testSuiteLevelLeak() {
    FullResult result = runTests(NestedWithSuiteLeak.class);
    // Both tests should run, but suite leak should be detected
    assertThat(result.getRunCount()).isEqualTo(2);
    // Suite leak is reported as container-level failure
    assertThat(result.getFailureCount()).isGreaterThanOrEqualTo(1);
    boolean hasSuiteLeak = result.getFailures().stream()
        .anyMatch(f -> f.getException().getMessage().contains("SUITE scope"));
    assertThat(hasSuiteLeak).isTrue();
  }
  
  @Test
  public void testLeakScopeNone() {
    FullResult result = runTests(NestedWithLeakScopeNone.class);
    assertThat(result.getRunCount()).isEqualTo(1);
    // No failure because leak detection is disabled
    assertThat(result.getFailureCount()).isEqualTo(0);
  }
}

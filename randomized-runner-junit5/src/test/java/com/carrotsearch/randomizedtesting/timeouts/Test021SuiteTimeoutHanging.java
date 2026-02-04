package com.carrotsearch.randomizedtesting.timeouts;

import com.carrotsearch.randomizedtesting.DeadlineClock;
import com.carrotsearch.randomizedtesting.RandomizedExtension;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.SysGlobals;
import com.carrotsearch.randomizedtesting.WithNestedTestClass;
import com.carrotsearch.randomizedtesting.annotations.*;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope.Scope;
import com.carrotsearch.randomizedtesting.extensions.TestCaseOrderingExtension;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests that the framework doesn't hang when a thread can't be interrupted.
 */
public class Test021SuiteTimeoutHanging extends WithNestedTestClass {
  private static AtomicBoolean stop;
  private static ArrayList<Thread> waitQueue = new ArrayList<>();

  @ExtendWith(RandomizedExtension.class)
  @ThreadLeakScope(Scope.SUITE)
  @ThreadLeakLingering(linger = 0)
  @ThreadLeakAction({ThreadLeakAction.Action.WARN, ThreadLeakAction.Action.INTERRUPT})
  @ThreadLeakZombies(ThreadLeakZombies.Consequence.IGNORE_REMAINING_TESTS)
  @TimeoutSuite(millis = 1000)
  @TestMethodOrder(TestCaseOrderingExtension.class)
  @TestCaseOrdering(TestCaseOrdering.AlphabeticOrder.class)
  public static class Nested1 extends RandomizedTest {
    @Test
    public void test001() throws Exception {
      synchronized (Thread.currentThread()) {
        waitQueue.add(Thread.currentThread());
        while (!stop.get()) {
          try {
            Thread.sleep(250);
          } catch (InterruptedException e) {
            // If interrupted, just continue. Don't release the lock.
            System.out.println("Interrupted.");
          }
          System.out.println("Still running.");
        }
      }
    }

    @Test
    public void test002() throws Exception {
      // Should not be executed.
      throw new Exception();
    }

    @BeforeAll
    static void setup() {
      assumeRunningNested();
    }
  }

  @AfterEach
  public void cleanup() {
    System.clearProperty(SysGlobals.SYSPROP_KILLATTEMPTS());
  }

  @Test
  public void testThreadLeakInterruptsIsNotHangingOnJoin() throws Throwable {
    stop = new AtomicBoolean();
    waitQueue.clear();

    System.setProperty(SysGlobals.SYSPROP_KILLATTEMPTS(), "1");
    AtomicReference<FullResult> result = new AtomicReference<>();
    Thread tester = new Thread(() -> {
      waitQueue.add(Thread.currentThread());
      result.set(runTests(Nested1.class));
    });
    tester.start();

    DeadlineClock deadlineClock = new DeadlineClock(TimeUnit.SECONDS, 10);
    while (deadlineClock.isBeforeDeadline() && tester.isAlive()) {
      Thread.sleep(250);
    }

    boolean testerAlive = tester.isAlive();

    // Wait for all threads to die.
    stop.set(true);
    for (Thread t : waitQueue) {
      t.join();
    }

    // Make sure the tester was dead when we left the long wait loop. This
    // indicates the framework abandoned the thread it couldn't interrupt.
    Assertions.assertThat(testerAlive).isFalse();

    // At least one test executed (in JUnit 5, test002 may also run but fail immediately
    // due to suite timeout already being exceeded)
    Assertions.assertThat(result.get().getRunCount()).isGreaterThanOrEqualTo(1);

    // Make sure suite timeouts have been reported (at least one failure should be a suite timeout).
    Assertions.assertThat(result.get().getFailures()).isNotEmpty();
    boolean hasSuiteTimeout = result.get().getFailures().stream()
        .anyMatch(f -> f.getException().getMessage().toLowerCase(Locale.ROOT).contains("suite timeout"));
    Assertions.assertThat(hasSuiteTimeout)
        .as("Expected at least one suite timeout failure")
        .isTrue();
  }
}

package com.carrotsearch.randomizedtesting.timeouts;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.RandomizedExtension;
import com.carrotsearch.randomizedtesting.WithNestedTestClass;

import static org.junit.jupiter.api.Assertions.*;

public class Test012RunawayThreadsKilledAtOnce extends WithNestedTestClass {
  @ExtendWith(RandomizedExtension.class)
  public static class NestedClass {
    @Test
    public void lotsOfStubbornThreads() throws Throwable {
      assumeRunningNested();
      final CountDownLatch latch = new CountDownLatch(50);

      Thread [] threads = new Thread [(int) latch.getCount()];
      for (int i = 0; i < threads.length; i++) {
        threads[i] = new Thread("stubborn-" + i) {
          @Override
          public void run() {
            latch.countDown();

            try {
              Thread.sleep(20000);
            } catch (InterruptedException e) {
              // Ignore
            }
          }
        };
        threads[i].start();
      }

      // Wait for all threads to be really started.
      latch.await();
    }
  }

  @Test
  public void testLotsOfStubbornThreads() {
    long start = System.nanoTime();
    FullResult result = runTests(NestedClass.class);
    long end = System.nanoTime();

    assertEquals(1, result.getFailureCount());
    long msec = TimeUnit.NANOSECONDS.toMillis(end - start);
    assertTrue(msec < 1000 * 10, msec + " msec?");
  }
}

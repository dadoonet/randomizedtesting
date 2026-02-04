package com.carrotsearch.randomizedtesting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RandomizedContext.
 */
@ExtendWith(RandomizedExtension.class)
public class TestRandomizedContext extends RandomizedTest {

  @Test
  public void testRunWithPrivateRandomness() throws Exception {
    final int iters = randomIntBetween(1, 10);
    for (int j = 0; j < iters; j++) {
      final long seed = randomLong();
      final int[] first = RandomizedContext.current().runWithPrivateRandomness(seed, new Callable<int[]>() {
        @Override
        public int[] call() throws Exception {
          final int size = randomIntBetween(10, 1000);
          final int[] result = new int[size];
          for (int i = 0; i < size; i++) {
            result[i] = randomInt();
          }
          return result;
        }
      });
      assertNotNull(first);
      final int[] second = RandomizedContext.current().runWithPrivateRandomness(seed, new Callable<int[]>() {
        @Override
        public int[] call() throws Exception {
          final int size = randomIntBetween(10, 1000);
          final int[] result = new int[size];
          for (int i = 0; i < size; i++) {
            result[i] = randomInt();
          }
          return result;
        }
      });
      assertNotNull(second);
      assertNotSame(first, second);
      assertArrayEquals(first, second, "first and second sequence must be identical");
    }
  }

}

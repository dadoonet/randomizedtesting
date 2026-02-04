package com.carrotsearch.randomizedtesting;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.annotations.Repeat;
import com.carrotsearch.randomizedtesting.extensions.RepeatExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Nightly mode checks.
 */
public class TestIterationsAnnotation extends RandomizedTest {
  static int iterations = 0;

  @TestTemplate
  @ExtendWith(RepeatExtension.class)
  @Repeat(iterations = 10)
  public void nightly() {
    iterations++;
  }

  @BeforeAll
  public static void clean() {
    iterations = 0;
  }
  
  @AfterAll
  public static void cleanupAfter() {
    assertEquals(10, iterations);
  }
}

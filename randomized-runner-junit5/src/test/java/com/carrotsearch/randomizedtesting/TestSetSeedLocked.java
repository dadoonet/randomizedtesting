package com.carrotsearch.randomizedtesting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestSetSeedLocked extends RandomizedTest {
  @Test
  public void testMethod() {
    try {
      getRandom().setSeed(0);
      fail();
    } catch (RuntimeException e) {
      // Ok, expected.
    }
  }
}

package com.carrotsearch.randomizedtesting;

import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Check out of scope {@link Random} use.
 */
public class TestRandomInStaticInitializer extends RandomizedTest {
  static boolean wasOutOfScope;

  static {
    try {
      RandomizedContext.current();
    } catch (IllegalStateException e) {
      wasOutOfScope = true;
    }
  }

  @Test
  public void testStaticInitializerOutOfScope() {
    assertTrue(wasOutOfScope);
  }
}

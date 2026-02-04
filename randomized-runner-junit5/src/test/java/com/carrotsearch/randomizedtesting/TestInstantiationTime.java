package com.carrotsearch.randomizedtesting;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Check that {@link BeforeClass} hooks are called before instance initializers.
 */
public class TestInstantiationTime extends RandomizedTest {
  
  private static String constant;

  /**
   * Instance initializer. Will result in an NPE if 
   * {@link #prepare()} is not invoked before.
   */
  public String copyOfStatic = constant.toUpperCase();
  
  @BeforeAll
  public static void prepare() {
    constant = "constant";
  }
  
  @Test
  public void testDummy() {
    // empty.
  }
}

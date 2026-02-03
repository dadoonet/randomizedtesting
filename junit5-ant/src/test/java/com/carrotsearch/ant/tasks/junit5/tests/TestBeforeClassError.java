package com.carrotsearch.ant.tasks.junit5.tests;

import org.junit.BeforeClass;
import org.junit.Test;

public class TestBeforeClassError {
  @BeforeClass
  public static void beforeClass() {
    throw new RuntimeException();
  }

  @Test
  public void method() {}
}

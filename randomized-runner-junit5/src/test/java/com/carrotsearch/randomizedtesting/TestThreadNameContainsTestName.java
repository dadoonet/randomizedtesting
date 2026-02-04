package com.carrotsearch.randomizedtesting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.annotations.Timeout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that thread name contains test method name.
 */
@ExtendWith(RandomizedExtension.class)
public class TestThreadNameContainsTestName extends RandomizedTest {
  @Test
  public void testMarkerABC() {
    String tName = Thread.currentThread().getName();
    assertTrue(tName.contains("testMarkerABC"), tName);
  }

  @Test
  @Timeout(millis = 0)
  public void testMarkerXYZ() {
    String tName = Thread.currentThread().getName();
    assertTrue(tName.contains("testMarkerXYZ"), tName);
  }

  @Test
  @Timeout(millis = 1000)
  public void testMarkerKJI() {
    String tName = Thread.currentThread().getName();
    assertTrue(tName.contains("testMarkerKJI"), tName);
  }    
}

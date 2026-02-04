package com.carrotsearch.randomizedtesting.generators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;

import static org.junit.jupiter.api.Assertions.*;

public class TestRandomPicks extends RandomizedTest {
  @Test
  public void testRandomFromEmptyCollection() {
    assertThrows(IllegalArgumentException.class, () -> {
      RandomPicks.randomFrom(getRandom(), new HashSet<Object>());
    });
  }

  @Test
  public void testRandomFromCollection() {
    Object t = new Object();
    Object r = RandomPicks.randomFrom(getRandom(), new HashSet<Object>(Arrays.asList(t)));
    assertSame(r, t);
  }

  @Test
  public void testRandomFromList() {
    assertThrows(IllegalArgumentException.class, () -> {
      RandomPicks.randomFrom(getRandom(), new ArrayList<Object>());
    });
  }

  @Test
  public void testRandomFromArray() {
    assertThrows(IllegalArgumentException.class, () -> {
      RandomPicks.randomFrom(getRandom(), new Object[] {});
    });
  }
}

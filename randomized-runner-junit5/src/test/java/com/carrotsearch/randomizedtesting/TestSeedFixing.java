package com.carrotsearch.randomizedtesting;

import static org.junit.jupiter.api.Assertions.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.annotations.Seed;

/**
 * Seed fixing for static fixtures and/or methods using annotations.
 */
@ExtendWith(RandomizedExtension.class)
@Seed("deadbeef")
public class TestSeedFixing {
  @BeforeAll
  public static void beforeClass() {
    assertEquals(0xdeadbeefL, RandomizedContext.current().getRandomness().getSeed());
  }

  @Seed("cafebabe")
  @Test
  public void dummy() {
    Assertions
      .assertThat(Long.toHexString(RandomizedContext.current().getRandomness().getSeed()))
      .isEqualTo("cafebabe");
  }
}

package com.carrotsearch.randomizedtesting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.annotations.Repeat;
import com.carrotsearch.randomizedtesting.extensions.RepeatExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(RandomizedExtension.class)
public class TestFormattingRandomSeeds {
  @Test
  public void minusOne() {
    check(-1L);
  }
  
  @Test
  public void zero() {
    check(0);
  }
  
  @Test
  public void maxLong() {
    check(Long.MAX_VALUE);
  }

  /** Heck, why not use ourselves here? ;) */
  @TestTemplate
  @ExtendWith(RepeatExtension.class)
  @Repeat(iterations = 1000)
  public void noise() {
    check(RandomizedContext.current().getRandom().nextLong());
  }

  private void check(long seed) {
    String asString = SeedUtils.formatSeedChain(new Randomness(seed, RandomSupplier.DEFAULT));
    assertEquals(seed, SeedUtils.parseSeedChain(asString)[0]);
  }
}

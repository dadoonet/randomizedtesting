package com.carrotsearch.randomizedtesting.generators;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.RandomizedExtension;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.Repeat;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Base class for testing {@link StringGenerator}s.
 */
@ExtendWith(RandomizedExtension.class)
public abstract class StringGeneratorTestBase extends RandomizedTest {
  protected final StringGenerator generator;

  protected StringGeneratorTestBase(StringGenerator generator) {
    this.generator = generator;
  }

  @Test @Repeat(iterations = 10)
  public void checkFixedCodePointLength() {
    int codepoints = iterationFix(randomIntBetween(1, 100));
    String s = generator.ofCodePointsLength(getRandom(), codepoints, codepoints);
    assertEquals(codepoints, s.codePointCount(0, s.length()), s);
  }

  @Test @Repeat(iterations = 10)
  public void checkRandomCodePointLength() {
    int from = iterationFix(randomIntBetween(1, 100));
    int to = from + randomIntBetween(0, 100);

    String s = generator.ofCodePointsLength(getRandom(), from, to);
    int codepoints = s.codePointCount(0, s.length());
    assertTrue(from <= codepoints && codepoints <= to,
        codepoints + " not within " + from + "-" + to);
  }

  @Test @Repeat(iterations = 10)
  public void checkFixedCodeUnitLength() {
    int codeunits = iterationFix(randomIntBetween(1, 100));
    String s = generator.ofCodeUnitsLength(getRandom(), codeunits, codeunits);
    assertEquals(codeunits, s.length(), s);
    assertEquals(codeunits, s.toCharArray().length, s);
  }

  @Test @Repeat(iterations = 10)
  public void checkRandomCodeUnitLength() {
    int from = iterationFix(randomIntBetween(1, 100));
    int to = from + randomIntBetween(0, 100);

    String s = generator.ofCodeUnitsLength(getRandom(), from, to);
    int codeunits = s.length();
    assertTrue(from <= codeunits && codeunits <= to,
        codeunits + " not within " + from + "-" + to);
  }

  @Test
  public void checkZeroLength() {
    assertEquals("", generator.ofCodePointsLength(getRandom(), 0, 0));
    assertEquals("", generator.ofCodeUnitsLength(getRandom(), 0, 0));
  }
  
  /**
   * Correct the count if a given generator doesn't support all possible values (in tests).
   */
  protected int iterationFix(int i) {
    return i;
  }
}

package com.carrotsearch.randomizedtesting.generators;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.RandomizedExtension;
import com.carrotsearch.randomizedtesting.RandomizedTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CodepointSetGenerator.
 */
@ExtendWith(RandomizedExtension.class)
public class TestCodepointSetGenerator extends RandomizedTest {
  private final static int [] codepoints = {
    'a', 'b', 'c', 'd',
    0xd7ff,
    0xffff,
    0x10000,
    0x1D11E,
    0x10FFFD,
  };

  private final static int [] surrogates = {
    0x10000,
    0x1D11E,
    0x10FFFD,
  };

  private final static String withSurrogates = new String(codepoints, 0, codepoints.length);

  @Nested
  public class CodepointSetOnChars extends StringGeneratorTestBase {
    public CodepointSetOnChars() {
      super(new CodepointSetGenerator(new char[] {
          'a', 'b', 'c', 'd',
          0x100,
          0xd7ff,
          0xffff
      }));
    }

    @Test
    public void testAllCharactersUsed() {
      char [] domain = "abcdefABCDEF".toCharArray();
      Set<Character> chars = new HashSet<Character>();
      for (char chr : domain) {
        chars.add(chr);
      }

      CodepointSetGenerator gen = new CodepointSetGenerator(domain);
      Random r = new Random(randomLong());
      for (int i = 0; i < 1000000 && !chars.isEmpty(); i++) {
        for (char ch : gen.ofCodeUnitsLength(r, 100, 100).toCharArray()) {
          chars.remove(ch);
        }
      }
      
      assertTrue(chars.isEmpty());
    }

    @Test
    public void testSurrogatesInConstructor() {
      assertThrows(IllegalArgumentException.class, () -> {
        new CodepointSetGenerator(withSurrogates.toCharArray());
      });
    }
  }

  @Nested
  public class CodepointSetOnCodePoints extends StringGeneratorTestBase {
    public CodepointSetOnCodePoints() {
      super(new CodepointSetGenerator(withSurrogates));      
    }
    
    @Test
    public void testAllCharactersUsed() {
      char [] domain = "abcdefABCDEF".toCharArray();
      Set<Character> chars = new HashSet<Character>();
      for (char chr : domain) {
        chars.add(chr);
      }

      CodepointSetGenerator gen = new CodepointSetGenerator(new String(domain));
      Random r = new Random(randomLong());
      for (int i = 0; i < 1000000 && !chars.isEmpty(); i++) {
        for (char ch : gen.ofCodeUnitsLength(r, 100, 100).toCharArray()) {
          chars.remove(ch);
        }
      }
      
      assertTrue(chars.isEmpty());
    }    
  }
  
  @Nested
  public class CodepointSetOnSurrogatesOnly extends StringGeneratorTestBase {
    public CodepointSetOnSurrogatesOnly() {
      super(new CodepointSetGenerator(new String(surrogates, 0, surrogates.length)));      
    }

    @Test
    public void testOddCodePoints() {
      assertThrows(IllegalArgumentException.class, () -> {
        generator.ofCodeUnitsLength(getRandom(), 3, 3);
      });
    }

    @Override
    protected int iterationFix(int i) {
      return i & ~1;      // Even only.
    }
  }  
}

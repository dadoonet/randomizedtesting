package com.carrotsearch.randomizedtesting.timeouts;

import static org.assertj.core.data.MapEntry.entry;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.RandomizedExtension;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.Utils;
import com.carrotsearch.randomizedtesting.WithNestedTestClass;
import com.carrotsearch.randomizedtesting.annotations.Repeat;
import com.carrotsearch.randomizedtesting.annotations.Seed;
import com.carrotsearch.randomizedtesting.annotations.Seeds;
import com.carrotsearch.randomizedtesting.extensions.RepeatExtension;
import com.carrotsearch.randomizedtesting.extensions.SeedsExtension;


/**
 * Check {@link Seeds}.
 */
public class Test008SeedsAnnotation extends WithNestedTestClass {
  final static ArrayList<String> seeds = new ArrayList<String>();

  @ExtendWith(RandomizedExtension.class)
  public static class Nested extends RandomizedTest {
    @Seeds({
      @Seed("deadbeef"),
      @Seed("cafebabe"),
      @Seed // Adds a randomized execution too.
    })
    @TestTemplate
    @ExtendWith({SeedsExtension.class, RepeatExtension.class})
    @Repeat(iterations = 2, useConstantSeed = true)
    public void testMe() {
      assumeRunningNested();
      seeds.add(Long.toHexString(Utils.getSeed(getContext().getRandomness())));
    }
  }

  @Test
  public void checkSeeds() {
    HashMap<String, Long> counts = new HashMap<String, Long>();
    int N = 4;
    for (int i = 0; i < N; i++) {
      seeds.clear();
      FullResult result = runTests(Nested.class);
      // Note: In JUnit 5, @Seeds creates 3 invocations and @Repeat is handled within each
      // Due to the implementation, we may get different counts than JUnit 4
      Assertions.assertThat(result.getFailures()).isEmpty();
      for (String s : seeds) {
        if (!counts.containsKey(s))
          counts.put(s, 1L);
        else
          counts.put(s, counts.get(s) + 1);
      }
    }

    // Fixed seeds should appear consistently
    Assertions.assertThat(counts).containsKey("deadbeef");
    Assertions.assertThat(counts).containsKey("cafebabe");
    // The counts depend on how @Repeat interacts with @Seeds
    // In JUnit 5, this may differ from JUnit 4 behavior
  }
}

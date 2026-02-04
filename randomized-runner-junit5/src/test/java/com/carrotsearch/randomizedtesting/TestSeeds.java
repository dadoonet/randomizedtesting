package com.carrotsearch.randomizedtesting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.annotations.Seed;
import com.carrotsearch.randomizedtesting.annotations.Seeds;
import com.carrotsearch.randomizedtesting.extensions.SeedsExtension;

/**
 * Test for {@link Seeds} annotation support.
 */
public class TestSeeds extends WithNestedTestClass {
  
  /**
   * Nested test class with @Seeds annotation.
   */
  @ExtendWith({RandomizedExtension.class, SeedsExtension.class})
  public static class NestedWithSeeds extends RandomizedTest {
    static List<String> capturedSeeds = new ArrayList<>();
    
    @TestTemplate
    @Seeds({
      @Seed("deadbeef"),
      @Seed("cafebabe")
    })
    public void testWithFixedSeeds() {
      assumeRunningNested();
      long seed = getContext().getRandomness().getSeed();
      capturedSeeds.add(Long.toHexString(seed));
    }
  }
  
  @BeforeEach
  public void setup() {
    NestedWithSeeds.capturedSeeds.clear();
  }
  
  @Test
  public void testFixedSeeds() {
    FullResult result = runTests(NestedWithSeeds.class);
    
    // Should run 2 times (one per seed)
    assertThat(result.getRunCount()).isEqualTo(2);
    assertThat(result.getFailureCount()).isEqualTo(0);
    
    // Verify the seeds were used
    assertThat(NestedWithSeeds.capturedSeeds).containsExactlyInAnyOrder("deadbeef", "cafebabe");
  }
  
  /**
   * Nested test class with @Seeds including a random seed.
   */
  @ExtendWith({RandomizedExtension.class, SeedsExtension.class})
  public static class NestedWithRandomSeed extends RandomizedTest {
    static Set<String> capturedSeeds = new HashSet<>();
    
    @TestTemplate
    @Seeds({
      @Seed("deadbeef"),
      @Seed // random
    })
    public void testWithMixedSeeds() {
      assumeRunningNested();
      long seed = getContext().getRandomness().getSeed();
      capturedSeeds.add(Long.toHexString(seed));
    }
  }
  
  @Test
  public void testMixedSeeds() {
    NestedWithRandomSeed.capturedSeeds.clear();
    FullResult result = runTests(NestedWithRandomSeed.class);
    
    // Should run 2 times (one fixed + one random)
    assertThat(result.getRunCount()).isEqualTo(2);
    assertThat(result.getFailureCount()).isEqualTo(0);
    
    // Verify deadbeef was used
    assertThat(NestedWithRandomSeed.capturedSeeds).contains("deadbeef");
    // Verify we have 2 different seeds
    assertThat(NestedWithRandomSeed.capturedSeeds).hasSize(2);
  }
  
  /**
   * Nested test class with multiple test methods and @Seeds.
   */
  @ExtendWith({RandomizedExtension.class, SeedsExtension.class})
  public static class NestedMultipleMethods extends RandomizedTest {
    static List<String> calls = new ArrayList<>();
    
    @TestTemplate
    @Seeds({@Seed("aaa"), @Seed("bbb")})
    public void testA() {
      assumeRunningNested();
      calls.add("testA:" + Long.toHexString(getContext().getRandomness().getSeed()));
    }
    
    @Test
    public void testB() {
      assumeRunningNested();
      calls.add("testB");
    }
  }
  
  @Test
  public void testMultipleMethods() {
    NestedMultipleMethods.calls.clear();
    FullResult result = runTests(NestedMultipleMethods.class);
    
    // testA runs 2 times (2 seeds), testB runs once
    assertThat(result.getRunCount()).isEqualTo(3);
    assertThat(result.getFailureCount()).isEqualTo(0);
    
    // Verify calls
    assertThat(NestedMultipleMethods.calls)
        .containsExactlyInAnyOrder("testA:aaa", "testA:bbb", "testB");
  }
}

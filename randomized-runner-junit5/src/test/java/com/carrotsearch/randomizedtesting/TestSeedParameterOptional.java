package com.carrotsearch.randomizedtesting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.annotations.*;
import com.carrotsearch.randomizedtesting.extensions.RepeatExtension;
import com.carrotsearch.randomizedtesting.extensions.SeedsExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Check that @Seed and @Seeds annotations work correctly with @Repeat.
 * 
 * Note: In JUnit 5, @Seeds with @Repeat uses TestTemplate which creates
 * separate test invocations.
 */
public class TestSeedParameterOptional extends WithNestedTestClass {

  @ExtendWith(RandomizedExtension.class)
  public static class Nested extends RandomizedTest {
    
    @BeforeEach
    public void assumeNested() {
      assumeRunningNested();
    }

    @TestTemplate
    @ExtendWith({SeedsExtension.class, RepeatExtension.class})
    @Seeds({
      @Seed("deadbeef"),
      @Seed("cafebabe"),
      @Seed // Adds a randomized execution too.
    })
    @Repeat(iterations = 2, useConstantSeed = true)
    public void method1() { }

    @Test
    public void method2() { }
    
    @Test
    public void method3() { }        
  }
  
  @Test
  public void checkTestCounts() {
    FullResult result = runTests(Nested.class);
    
    // method2 and method3 each run once
    // method1 with 3 seeds + 2 repeats = 5 invocations (sequential, not multiplicative)
    // Total: 5 + 1 + 1 = 7
    // Note: In JUnit 5, @Seeds and @Repeat are separate extensions that add invocations
    // sequentially rather than multiplying them like in JUnit 4
    assertThat(result.getRunCount()).isEqualTo(7);
    assertThat(result.getFailureCount()).isZero();
  }

  /**
   * Test that @Seed on a regular @Test method works.
   */
  @ExtendWith(RandomizedExtension.class)
  public static class NestedSingleSeed extends RandomizedTest {
    static long observedSeed;
    
    @BeforeEach
    public void assumeNested() {
      assumeRunningNested();
    }

    @Test
    @Seed("cafebabe")
    public void methodWithSeed() {
      observedSeed = RandomizedContext.current().getRandomness().getSeed();
    }
  }
  
  @Test
  public void testSingleSeedAnnotation() {
    NestedSingleSeed.observedSeed = 0;
    runTests(NestedSingleSeed.class);
    // Seed "cafebabe" = 0xcafebabe = 3405691582L
    assertThat(NestedSingleSeed.observedSeed).isEqualTo(0xcafebabeL);
  }
}

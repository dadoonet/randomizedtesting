package com.carrotsearch.randomizedtesting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.annotations.Repeat;
import com.carrotsearch.randomizedtesting.extensions.RepeatExtension;

/**
 * Test for {@link Repeat} annotation support.
 */
public class TestRepeat extends WithNestedTestClass {
  
  /**
   * Nested test class using @Repeat with @TestTemplate
   */
  @ExtendWith({RandomizedExtension.class, RepeatExtension.class})
  public static class NestedWithRepeat extends RandomizedTest {
    static AtomicInteger counter = new AtomicInteger();
    
    @TestTemplate
    @Repeat(iterations = 5)
    public void repeatedTest() {
      assumeRunningNested();
      counter.incrementAndGet();
    }
    
    @Test
    public void normalTest() {
      assumeRunningNested();
      counter.incrementAndGet();
    }
  }
  
  @BeforeEach
  public void setup() {
    NestedWithRepeat.counter.set(0);
  }
  
  @Test
  public void testRepeatWithTestTemplate() {
    // 5 repetitions + 1 normal test = 6 runs
    FullResult result = checkTestsOutput(6, 0, 0, 0, NestedWithRepeat.class);
    assertThat(result.wasSuccessful()).isTrue();
    assertThat(NestedWithRepeat.counter.get()).isEqualTo(6);
  }
  
  /**
   * Nested test class using JUnit 5's native @RepeatedTest (recommended approach)
   */
  @ExtendWith(RandomizedExtension.class)
  public static class NestedWithRepeatedTest extends RandomizedTest {
    static AtomicInteger counter = new AtomicInteger();
    
    @org.junit.jupiter.api.RepeatedTest(5)
    public void repeatedTest() {
      assumeRunningNested();
      counter.incrementAndGet();
    }
  }
  
  @Test
  public void testNativeRepeatedTest() {
    NestedWithRepeatedTest.counter.set(0);
    // 5 repetitions
    FullResult result = checkTestsOutput(5, 0, 0, 0, NestedWithRepeatedTest.class);
    assertThat(result.wasSuccessful()).isTrue();
    assertThat(NestedWithRepeatedTest.counter.get()).isEqualTo(5);
  }
}

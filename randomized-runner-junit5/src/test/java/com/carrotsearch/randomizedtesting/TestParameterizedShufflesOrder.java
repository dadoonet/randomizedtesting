package com.carrotsearch.randomizedtesting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import com.carrotsearch.randomizedtesting.extensions.ParametersFactoryExtension;

/**
 * Tests for @ParametersFactory shuffle option.
 * 
 * Note: In JUnit 5, class-level @ParametersFactory doesn't automatically multiply
 * test executions like in JUnit 4. Each test method gets one parameter set,
 * cycling through available parameters. This test verifies that parameter shuffling
 * affects the order in which parameters are assigned to test methods.
 */
public class TestParameterizedShufflesOrder extends WithNestedTestClass {
  static List<Integer> creationOrder;

  @ExtendWith({RandomizedExtension.class, ParametersFactoryExtension.class})
  public static class NoShuffle extends RandomizedTest {
    private final int value;

    public NoShuffle(int value) {
      this.value = value;
      if (isRunningNested()) {
        creationOrder.add(value);
      }
    }

    @BeforeAll
    public static void checkNested() {
      assumeRunningNested();
    }

    @Test public void test0() {}
    @Test public void test1() {}
    @Test public void test2() {}
    @Test public void test3() {}
    @Test public void test4() {}

    @ParametersFactory(shuffle = false)
    public static Iterable<Object[]> parameters() {
      List<Object[]> params = new ArrayList<>();
      for (int i = 0; i < 5; i++) {
        params.add($(i));
      }
      return params;
    }
  }

  @ExtendWith({RandomizedExtension.class, ParametersFactoryExtension.class})
  public static class WithShuffle extends RandomizedTest {
    private final int value;

    public WithShuffle(int value) {
      this.value = value;
      if (isRunningNested()) {
        creationOrder.add(value);
      }
    }

    @BeforeAll
    public static void checkNested() {
      assumeRunningNested();
    }

    @Test public void test0() {}
    @Test public void test1() {}
    @Test public void test2() {}
    @Test public void test3() {}
    @Test public void test4() {}

    @ParametersFactory(shuffle = true)
    public static Iterable<Object[]> parameters() {
      List<Object[]> params = new ArrayList<>();
      for (int i = 0; i < 5; i++) {
        params.add($(i));
      }
      return params;
    }
  }

  @BeforeEach
  public void setup() {
    creationOrder = new ArrayList<>();
  }
  
  @Test
  public void testWithoutShuffle() {
    // Run multiple times - without shuffle, parameter order should be deterministic
    Set<String> ordersSeen = new HashSet<>();
    for (int i = 0; i < 5; i++) {
      creationOrder = new ArrayList<>();
      runTests(NoShuffle.class);
      ordersSeen.add(creationOrder.toString());
    }
    // Without shuffle, all runs should have the same order
    Assertions.assertThat(ordersSeen).hasSize(1);
    // Order should be sequential: 0, 1, 2, 3, 4 (one per test method, cycling)
    Assertions.assertThat(creationOrder).containsExactly(0, 1, 2, 3, 4);
  }
  
  @Test
  public void testWithShuffle() {
    // Run multiple times - with shuffle, parameter order should vary
    Set<String> ordersSeen = new HashSet<>();
    for (int i = 0; i < 10; i++) {
      creationOrder = new ArrayList<>();
      runTests(WithShuffle.class);
      ordersSeen.add(creationOrder.toString());
    }
    // With shuffle, we should see different orders across runs
    // (statistically unlikely to get same order 10 times with 5! = 120 permutations)
    Assertions.assertThat(ordersSeen.size()).isGreaterThan(1);
  }
}

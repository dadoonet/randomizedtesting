package com.carrotsearch.randomizedtesting.inheritance;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.RandomizedExtension;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.WithNestedTestClass;

/**
 * Test private hooks propagation in class hierarchies.
 */
public class PrivateHooksPropagation extends WithNestedTestClass {
  final static List<String> order = new ArrayList<>();

  @ExtendWith(RandomizedExtension.class)
  public static class Super extends RandomizedTest {
    @BeforeAll private   static void beforeClass1() { order.add("super.beforeclass1"); }
    @BeforeAll protected static void beforeClass2() { order.add("super.beforeclass2"); }

    @BeforeEach private   void before1() { order.add("super.before1"); }
    @BeforeEach protected void before2() { order.add("super.before2"); }

    @Test public void testMethod1() { order.add("super.testMethod1"); }
  }

  @ExtendWith(RandomizedExtension.class)
  public static class Sub extends Super {
    @BeforeAll private   static void beforeClass1() { order.add("sub.beforeclass1"); }
    @BeforeAll protected static void beforeClass2() { order.add("sub.beforeclass2"); }

    @BeforeEach private   void before1() { order.add("sub.before1"); }
    @BeforeEach protected void before2() { order.add("sub.before2"); }
  }

  @Test
  public void checkPrivateHooks() throws Exception {
    order.clear();
    FullResult result = runTests(Sub.class);
    
    Assertions.assertThat(result.getRunCount()).isGreaterThan(0);
    Assertions.assertThat(result.getFailureCount()).isEqualTo(0);
    
    // Private methods with @BeforeAll/@BeforeEach are inherited but not overridden
    // Each class's private methods should be called
    Assertions.assertThat(order).contains("super.beforeclass1");
    Assertions.assertThat(order).contains("sub.beforeclass1");
    Assertions.assertThat(order).contains("sub.beforeclass2");
    Assertions.assertThat(order).contains("super.before1");
    Assertions.assertThat(order).contains("sub.before1");
    Assertions.assertThat(order).contains("sub.before2");
    Assertions.assertThat(order).contains("super.testMethod1");
  }
}

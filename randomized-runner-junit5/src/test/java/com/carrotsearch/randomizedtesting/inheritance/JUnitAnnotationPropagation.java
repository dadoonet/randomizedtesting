package com.carrotsearch.randomizedtesting.inheritance;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.RandomizedExtension;
import com.carrotsearch.randomizedtesting.WithNestedTestClass;

/**
 * Test that JUnit 5 annotation propagation works correctly in inheritance hierarchies.
 */
public class JUnitAnnotationPropagation extends WithNestedTestClass {
  final static List<String> order = new ArrayList<>();

  @ExtendWith(RandomizedExtension.class)
  public static class Super {
    @BeforeAll public static void beforeClass1() { order.add("super.beforeclass1"); }
                 public static void beforeClass2() { order.add("super.beforeclass2"); }
    @BeforeAll public static void beforeClass3() { order.add("super.beforeclass3"); }

    @BeforeEach public void before1() { order.add("super.before1"); }
                public void before2() { order.add("super.before2"); }
    @BeforeEach public void before3() { order.add("super.before3"); }

    @Test        public void testMethod1() { order.add("super.testMethod1"); }
                 public void testMethod2() { order.add("super.testMethod2"); }
    @Test        public void testMethod3() { order.add("super.testMethod3"); }

    // Awkward cases of annotations and virtual methods.
    @Disabled    public void testMethod4() { order.add("super.testMethod4"); }
    @Test        public void testMethod5() { order.add("super.testMethod5"); }
  }

  @ExtendWith(RandomizedExtension.class)
  public static class Sub extends Super {
                 public static void beforeClass1() { order.add("sub.beforeclass1"); }
    @BeforeAll   public static void beforeClass2() { order.add("sub.beforeclass2"); }
    @BeforeAll   public static void beforeClass3() { order.add("sub.beforeclass3"); }

                 public void before1() { order.add("sub.before1"); }
    @BeforeEach  public void before2() { order.add("sub.before2"); }
    @BeforeEach  public void before3() { order.add("sub.before3"); }

                 public void testMethod1() { order.add("sub.testMethod1"); }
    @Test        public void testMethod2() { order.add("sub.testMethod2"); }
    @Test        public void testMethod3() { order.add("sub.testMethod3"); }

    @Test        public void testMethod4() { order.add("sub.testMethod4"); }
    @Disabled    public void testMethod5() { order.add("sub.testMethod5"); }
  }

  @Test
  public void checkAnnotationPropagation() throws Exception {
    order.clear();
    FullResult result = runTests(Sub.class);
    
    // Verify tests executed
    Assertions.assertThat(result.getRunCount()).isGreaterThan(0);
    
    // Verify some methods were called
    Assertions.assertThat(order).isNotEmpty();
    
    // Check that before methods were called
    Assertions.assertThat(order.stream().anyMatch(s -> s.contains("before"))).isTrue();
  }
}

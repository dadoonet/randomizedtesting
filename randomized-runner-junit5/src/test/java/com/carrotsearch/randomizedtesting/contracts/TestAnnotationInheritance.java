package com.carrotsearch.randomizedtesting.contracts;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.RandomizedExtension;
import com.carrotsearch.randomizedtesting.WithNestedTestClass;

/**
 * Verify if annotations are inherited in JUnit 5.
 */
public class TestAnnotationInheritance extends WithNestedTestClass {
  final static List<String> order = new ArrayList<>();
  
  @ExtendWith(RandomizedExtension.class)
  public static class Nested1 {
    @BeforeAll
    public static void beforeClass() {
      order.add("before-class");
    }

    @BeforeEach
    public void before() {
      order.add("before-test");
    }
    
    @Test
    public void testMethod1() {
      order.add("testMethod1");
    }

    @AfterEach
    public void after() {
      order.add("after-test");
    }
    
    @AfterAll
    public static void afterClass() {
      order.add("after-class");
    }
  }
  
  @ExtendWith(RandomizedExtension.class)
  public static class Nested2 extends Nested1 {
    public static void beforeClass() {
      order.add("shadowed-before-class");
    }

    @Override
    public void before() {
      order.add("inherited before-test");
    }
    
    @Override
    public void after() {
      order.add("inherited after-test");
    }
    
    @Override
    public void testMethod1() {
      order.add("inherited testMethod");
    }

    public static void afterClass() {
      order.add("shadowed-after-class");
    }    
  }

  @Test
  public void checkAnnotationInheritance() throws Exception {
    order.clear();
    FullResult result = runTests(Nested2.class);
    
    // Verify test ran successfully
    Assertions.assertThat(result.getRunCount()).isGreaterThan(0);
    Assertions.assertThat(result.getFailureCount()).isEqualTo(0);
    
    // Verify lifecycle methods were called
    Assertions.assertThat(order).contains("before-class");
    Assertions.assertThat(order).contains("inherited before-test");
    Assertions.assertThat(order).contains("inherited testMethod");
    Assertions.assertThat(order).contains("inherited after-test");
    Assertions.assertThat(order).contains("after-class");
    
    // Shadowed static methods without @BeforeAll/@AfterAll should NOT be called
    Assertions.assertThat(order).doesNotContain("shadowed-before-class");
    Assertions.assertThat(order).doesNotContain("shadowed-after-class");
  }
}

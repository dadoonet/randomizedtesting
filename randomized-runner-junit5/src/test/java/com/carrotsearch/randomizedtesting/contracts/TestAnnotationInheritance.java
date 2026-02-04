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
 * Verify annotation inheritance behavior in JUnit 5.
 * 
 * Key differences from JUnit 4:
 * - @Test is NOT inherited on overridden methods (must re-annotate)
 * - @BeforeEach/@AfterEach are NOT inherited on overridden methods (must re-annotate)
 * - @BeforeAll/@AfterAll on static methods are NOT called if shadowed
 *   (static methods don't override, they shadow)
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
  
  /**
   * Subclass that properly inherits behavior without shadowing static methods.
   */
  @ExtendWith(RandomizedExtension.class)
  public static class Nested2 extends Nested1 {
    // Note: We don't shadow beforeClass() - in JUnit 5, shadowing breaks inheritance
    // The parent's @BeforeAll beforeClass() will be called

    @Override
    @BeforeEach  // Must re-annotate in JUnit 5 - not inherited
    public void before() {
      order.add("inherited before-test");
    }
    
    @Override
    @AfterEach  // Must re-annotate in JUnit 5 - not inherited
    public void after() {
      order.add("inherited after-test");
    }
    
    @Override
    @Test  // @Test must be re-applied in JUnit 5 - it's not inherited
    public void testMethod1() {
      order.add("inherited testMethod");
    }
    
    // Note: We don't shadow afterClass() - the parent's will be called
  }

  @Test
  public void checkAnnotationInheritance() throws Exception {
    order.clear();
    FullResult result = runTests(Nested2.class);
    
    // Verify test ran successfully
    Assertions.assertThat(result.getRunCount()).isGreaterThan(0);
    Assertions.assertThat(result.getFailureCount()).isEqualTo(0);
    
    // Verify lifecycle methods were called
    // In JUnit 5: parent's @BeforeAll is called (not shadowed)
    Assertions.assertThat(order).contains("before-class");
    // @BeforeEach with re-annotation on overridden method
    Assertions.assertThat(order).contains("inherited before-test");
    // Test method (must have @Test annotation in subclass)
    Assertions.assertThat(order).contains("inherited testMethod");
    // @AfterEach with re-annotation on overridden method
    Assertions.assertThat(order).contains("inherited after-test");
    // In JUnit 5: parent's @AfterAll is called (not shadowed)
    Assertions.assertThat(order).contains("after-class");
  }
}

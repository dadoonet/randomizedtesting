package com.carrotsearch.randomizedtesting.contracts;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.carrotsearch.randomizedtesting.RandomizedExtension;
import com.carrotsearch.randomizedtesting.WithNestedTestClass;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Class-level extension support (equivalent to JUnit 4 ClassRule).
 */
public class TestClassRules extends WithNestedTestClass {
  final static List<String> order = new ArrayList<>();
  
  /**
   * Extension that replaces the JUnit 4 ClassRule
   */
  public static class ClassRuleEquivalent implements BeforeAllCallback, AfterAllCallback {
    @Override
    public void beforeAll(ExtensionContext context) {
      order.add("rule-before");
    }
    
    @Override
    public void afterAll(ExtensionContext context) {
      order.add("rule-after");
    }
  }

  @ExtendWith({RandomizedExtension.class, ClassRuleEquivalent.class})
  public static class ClassRuleSupport {
    @BeforeAll
    public static void beforeClass() {
      order.add("before-class");
    }

    @AfterAll
    public static void afterClass() {
      order.add("after-class");
    }

    @Test
    public void passing() {
      order.add("passing");
    }
  }
  
  @Test
  public void checkClassExtension() throws Exception {
    order.clear();
    FullResult result = runTests(ClassRuleSupport.class);
    
    // Verify test passed
    assertEquals(1, result.getRunCount());
    assertEquals(0, result.getFailureCount());
    
    // Verify extension methods were called in correct order
    Assertions.assertThat(order).containsSequence("rule-before", "before-class");
    Assertions.assertThat(order).contains("passing");
    Assertions.assertThat(order).containsSequence("after-class", "rule-after");
  }
}

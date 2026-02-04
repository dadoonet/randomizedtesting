package com.carrotsearch.randomizedtesting.extensions;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.MethodOrdererContext;

import com.carrotsearch.randomizedtesting.TestMethodAndParams;
import com.carrotsearch.randomizedtesting.annotations.TestCaseOrdering;

/**
 * A JUnit 5 {@link MethodOrderer} that supports the {@link TestCaseOrdering} annotation
 * from the randomized testing framework.
 * 
 * <p>Usage:
 * <pre>
 * {@literal @}TestMethodOrder(TestCaseOrderingExtension.class)
 * {@literal @}TestCaseOrdering(TestCaseOrdering.AlphabeticOrder.class)
 * public class MyTest {
 *   // tests will be ordered alphabetically
 * }
 * </pre>
 */
public class TestCaseOrderingExtension implements MethodOrderer {
  
  /**
   * Simple implementation of TestMethodAndParams for ordering purposes.
   */
  private static class SimpleTestMethodAndParams implements TestMethodAndParams {
    private final Method method;
    private final List<Object> args;
    
    SimpleTestMethodAndParams(Method method) {
      this(method, Collections.emptyList());
    }
    
    SimpleTestMethodAndParams(Method method, List<Object> args) {
      this.method = method;
      this.args = args;
    }
    
    @Override
    public Method getTestMethod() {
      return method;
    }
    
    @Override
    public List<Object> getInstanceArguments() {
      return args;
    }
  }
  
  @Override
  public void orderMethods(MethodOrdererContext context) {
    Class<?> testClass = context.getTestClass();
    TestCaseOrdering annotation = testClass.getAnnotation(TestCaseOrdering.class);
    
    if (annotation == null) {
      // No @TestCaseOrdering, keep default order
      return;
    }
    
    try {
      // Instantiate the comparator
      Comparator<TestMethodAndParams> comparator = annotation.value().getDeclaredConstructor().newInstance();
      
      // Sort methods using the comparator
      context.getMethodDescriptors().sort((md1, md2) -> {
        // Create TestMethodAndParams wrappers (without instance arguments for simple case)
        TestMethodAndParams tmp1 = new SimpleTestMethodAndParams(md1.getMethod());
        TestMethodAndParams tmp2 = new SimpleTestMethodAndParams(md2.getMethod());
        return comparator.compare(tmp1, tmp2);
      });
      
    } catch (Exception e) {
      throw new RuntimeException("Failed to instantiate TestCaseOrdering comparator: " + annotation.value(), e);
    }
  }
}

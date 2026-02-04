package com.carrotsearch.randomizedtesting.extensions;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.carrotsearch.randomizedtesting.FilterExpressionParser;
import com.carrotsearch.randomizedtesting.FilterExpressionParser.IContext;
import com.carrotsearch.randomizedtesting.FilterExpressionParser.Node;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.SysGlobals;
import com.carrotsearch.randomizedtesting.annotations.TestGroup;

/**
 * JUnit 5 ExecutionCondition that evaluates @TestGroup annotations.
 * 
 * <p>This condition checks if a test should be executed based on:
 * <ul>
 *   <li>TestGroup annotations on the test class and method</li>
 *   <li>System properties that enable/disable groups</li>
 *   <li>Filter expressions via {@link SysGlobals#SYSPROP_TESTFILTER()}</li>
 * </ul>
 */
public class TestGroupCondition implements ExecutionCondition {

  private static class TestGroupInfo {
    final String name;
    final String sysProperty;
    final boolean enabled;

    TestGroupInfo(Class<? extends Annotation> annType) {
      TestGroup group = annType.getAnnotation(TestGroup.class);
      this.name = TestGroup.Utilities.getGroupName(annType);
      this.sysProperty = TestGroup.Utilities.getSysProperty(annType);
      
      boolean enabled;
      try {
        enabled = RandomizedTest.systemPropertyAsBoolean(sysProperty, group.enabled());
      } catch (IllegalArgumentException e) {
        // Ignore malformed system property, disable the group if malformed
        enabled = false;
      }
      this.enabled = enabled;
    }
  }

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    // Collect annotations from class and method
    Optional<Class<?>> testClass = context.getTestClass();
    Optional<Method> testMethod = context.getTestMethod();
    
    Map<String, TestGroupInfo> groups = new HashMap<>();
    
    if (testClass.isPresent()) {
      collectTestGroups(testClass.get(), groups);
    }
    if (testMethod.isPresent()) {
      collectTestGroups(testMethod.get(), groups);
    }
    
    // If no test groups, test is enabled
    if (groups.isEmpty()) {
      return ConditionEvaluationResult.enabled("No @TestGroup annotations");
    }
    
    // Check if any group is disabled
    String disabledReason = null;
    for (TestGroupInfo info : groups.values()) {
      if (!info.enabled) {
        disabledReason = "'" + info.name + "' test group is disabled";
        break;
      }
    }
    
    // Check filter expression
    String filterExpression = System.getProperty(SysGlobals.SYSPROP_TESTFILTER());
    if (filterExpression != null && !filterExpression.trim().isEmpty()) {
      Node filter = new FilterExpressionParser().parse(filterExpression);
      final String defaultState = disabledReason;
      
      boolean enabled = filter.evaluate(new IContext() {
        @Override
        public boolean defaultValue() {
          return defaultState == null;
        }

        @Override
        public boolean hasGroup(String value) {
          if (value.startsWith("@")) value = value.substring(1);
          for (TestGroupInfo info : groups.values()) {
            if (value.equalsIgnoreCase(info.name)) {
              return true;
            }
          }
          return false;
        }
      });
      
      if (enabled) {
        return ConditionEvaluationResult.enabled("Test filter allows execution");
      } else {
        return ConditionEvaluationResult.disabled("Test filter condition: " + filterExpression);
      }
    }
    
    // Return based on default group state
    if (disabledReason != null) {
      return ConditionEvaluationResult.disabled(disabledReason);
    }
    
    return ConditionEvaluationResult.enabled("All test groups are enabled");
  }
  
  private void collectTestGroups(AnnotatedElement element, Map<String, TestGroupInfo> groups) {
    for (Annotation ann : element.getAnnotations()) {
      Class<? extends Annotation> annType = ann.annotationType();
      if (annType.isAnnotationPresent(TestGroup.class)) {
        TestGroupInfo info = new TestGroupInfo(annType);
        groups.put(info.name, info);
      }
    }
  }
}

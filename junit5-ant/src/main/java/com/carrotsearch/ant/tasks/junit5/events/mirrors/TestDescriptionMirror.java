package com.carrotsearch.ant.tasks.junit5.events.mirrors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A serializable mirror of test description information.
 * This class replaces JUnit 4's {@code Description} with a framework-independent
 * representation that can be used with both JUnit 4 and JUnit 5.
 */
public class TestDescriptionMirror {
  private final String displayName;
  private final String className;
  private final String methodName;
  private final String uniqueId;
  private final List<TestDescriptionMirror> children;

  public TestDescriptionMirror(String displayName, String className, String methodName, String uniqueId) {
    this(displayName, className, methodName, uniqueId, new ArrayList<>());
  }

  public TestDescriptionMirror(String displayName, String className, String methodName, String uniqueId, 
                                List<TestDescriptionMirror> children) {
    this.displayName = displayName;
    this.className = className;
    this.methodName = methodName;
    this.uniqueId = uniqueId;
    this.children = children != null ? new ArrayList<>(children) : new ArrayList<>();
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getClassName() {
    return className;
  }

  public String getMethodName() {
    return methodName;
  }

  public String getUniqueId() {
    return uniqueId;
  }

  public List<TestDescriptionMirror> getChildren() {
    return Collections.unmodifiableList(children);
  }

  public void addChild(TestDescriptionMirror child) {
    children.add(child);
  }

  /**
   * Returns true if this is a test (has a method name), false if it's a container/suite.
   */
  public boolean isTest() {
    return methodName != null && !methodName.isEmpty();
  }

  /**
   * Returns true if this is a container/suite (has no method name or has children).
   */
  public boolean isSuite() {
    return !isTest() || !children.isEmpty();
  }

  /**
   * Creates a description for a test class (suite).
   */
  public static TestDescriptionMirror createSuiteDescription(Class<?> clazz) {
    return new TestDescriptionMirror(
        clazz.getName(),
        clazz.getName(),
        null,
        "class:" + clazz.getName()
    );
  }

  /**
   * Creates a description for a test class (suite) by name.
   */
  public static TestDescriptionMirror createSuiteDescription(String className) {
    return new TestDescriptionMirror(
        className,
        className,
        null,
        "class:" + className
    );
  }

  /**
   * Creates a description for a test method.
   */
  public static TestDescriptionMirror createTestDescription(Class<?> clazz, String methodName) {
    String displayName = methodName + "(" + clazz.getName() + ")";
    return new TestDescriptionMirror(
        displayName,
        clazz.getName(),
        methodName,
        "method:" + clazz.getName() + "#" + methodName
    );
  }

  /**
   * Creates a description for a test method by name.
   */
  public static TestDescriptionMirror createTestDescription(String className, String methodName) {
    String displayName = methodName + "(" + className + ")";
    return new TestDescriptionMirror(
        displayName,
        className,
        methodName,
        "method:" + className + "#" + methodName
    );
  }

  /**
   * Creates a description with a custom display name.
   */
  public static TestDescriptionMirror createDescription(String displayName, String className, String methodName) {
    String uniqueId;
    if (methodName != null && !methodName.isEmpty()) {
      uniqueId = "method:" + className + "#" + methodName;
    } else {
      uniqueId = "class:" + className;
    }
    return new TestDescriptionMirror(displayName, className, methodName, uniqueId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TestDescriptionMirror that = (TestDescriptionMirror) o;
    return Objects.equals(uniqueId, that.uniqueId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uniqueId);
  }

  @Override
  public String toString() {
    return displayName;
  }
}

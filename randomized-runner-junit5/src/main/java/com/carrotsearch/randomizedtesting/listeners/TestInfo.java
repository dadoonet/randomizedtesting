package com.carrotsearch.randomizedtesting.listeners;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Information about a test being executed.
 * This is a simplified alternative to JUnit Platform's TestIdentifier
 * that can be easily created from an ExtensionContext.
 */
public interface TestInfo {
  
  /**
   * Returns the display name for this test.
   */
  String getDisplayName();
  
  /**
   * Returns the unique identifier for this test.
   */
  String getUniqueId();
  
  /**
   * Returns the test class, if available.
   */
  Optional<Class<?>> getTestClass();
  
  /**
   * Returns the test method, if available.
   */
  Optional<Method> getTestMethod();
  
  /**
   * Returns whether this is a test (as opposed to a container).
   */
  boolean isTest();
}

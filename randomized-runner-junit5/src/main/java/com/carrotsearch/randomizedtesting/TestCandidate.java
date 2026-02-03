package com.carrotsearch.randomizedtesting;

import java.lang.reflect.Method;

/**
 * Represents a test method candidate for execution.
 */
public interface TestCandidate {
  /**
   * @return The test method.
   */
  Method getMethod();

  /**
   * @return The test class.
   */
  Class<?> getTestClass();

  /**
   * Convenience accessor for the method.
   */
  default Method method() {
    return getMethod();
  }
}

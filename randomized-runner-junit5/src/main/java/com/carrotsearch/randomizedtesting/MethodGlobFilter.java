package com.carrotsearch.randomizedtesting;

/**
 * A filter for method names using glob patterns.
 */
public class MethodGlobFilter extends GlobFilter {
  public MethodGlobFilter(String globPattern) {
    super(globPattern);
  }
  
  /**
   * Check if a method name should be included.
   * @param methodName the method name to check (can be null)
   * @return true if the method name matches or is null
   */
  public boolean shouldRun(String methodName) {
    return methodName == null || globMatches(methodName);
  }

  @Override
  public String describe() {
    return "Method matches: " + globPattern;
  }
}

package com.carrotsearch.randomizedtesting;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Historical rants about JUnit limitations. Many of these are resolved in JUnit 5,
 * but this file is kept for reference.
 */
final class Rants {
  enum RantType {
    // General
    ANNOYANCE,
    DAMN_TERRIBLE,
    WTF,
    RESOLVED_IN_JUNIT5,

    // Personal
    ISHOULDHAVEBECOMEALAWYER
  }
  
  /**
   * JUnit 4 issue: there was no way to carry test class/test name
   * separately from the display name.
   * 
   * <p>JUnit 5 has better support for this with TestIdentifier and display names.
   */
  public static RantType RANT_1 = RantType.RESOLVED_IN_JUNIT5;
  
  /**
   * JUnit 4 issue: Default assumption methods did not allow specifying a custom message.
   * 
   * <p>JUnit 5 Assumptions has better API.
   */
  public static RantType RANT_2 = RantType.RESOLVED_IN_JUNIT5;
  
  /**
   * JUnit 4 issue: Failed assumption propagated as a Failure.
   * 
   * <p>JUnit 5 properly distinguishes between test failures and aborted tests.
   */
  public static RantType RANT_3 = RantType.RESOLVED_IN_JUNIT5;

  /**
   * JUnit was inconsistent in how it treats annotations on methods. Some of them are "inherited" and
   * some require presence on the exact same {@link Method} as the one used for testing. This has awkward
   * side effects, for example {@link Disabled} and {@link Test} must co-exist on the same method, not
   * on virtual method hierarchy. Shadowing of {@link BeforeAll} methods can be inconsistent.
   * 
   * <p>JUnit 5 has improved handling but some edge cases remain.
   */
  public static RantType RANT_4 = RantType.ANNOYANCE;
}

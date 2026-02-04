package com.carrotsearch.ant.tasks.junit5.events.mirrors;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.opentest4j.TestAbortedException;

/**
 * A serializable mirror of test failure information.
 * This class provides a framework-independent representation of test failures
 * that can be used with both JUnit 4 and JUnit 5.
 */
public class FailureMirror {
  private String message;
  private String trace;
  private String throwableString;
  private String throwableClass;

  /** Was the failure an instance of an {@link AssertionError}? */
  private boolean assertionViolation;
  private boolean assumptionViolation;

  /** The test description that caused this failure. */
  private TestDescriptionMirror description;

  public FailureMirror(TestDescriptionMirror description,
                       String message, 
                       String trace,
                       String throwableString,
                       String throwableClass,
                       boolean assertionViolation,
                       boolean assumptionViolation) {
    this.message = message;
    this.trace = trace;
    this.throwableString = throwableString;
    this.throwableClass = throwableClass;
    this.assertionViolation = assertionViolation;
    this.assumptionViolation = assumptionViolation;
    this.description = description;
  }

  /**
   * Creates a FailureMirror from a description and throwable.
   */
  public FailureMirror(TestDescriptionMirror description, Throwable cause) {
    this.description = description;
    this.message = cause.getMessage();
    this.trace = getStackTrace(cause);
    this.assertionViolation = cause instanceof AssertionError;
    this.assumptionViolation = cause instanceof TestAbortedException 
        || isJUnit4AssumptionViolation(cause);
    this.throwableString = cause.toString();
    this.throwableClass = cause.getClass().getName();
  }

  private static String getStackTrace(Throwable t) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    return sw.toString();
  }

  /**
   * Check if the throwable is a JUnit 4 AssumptionViolatedException.
   * We check by class name to avoid a compile-time dependency on JUnit 4.
   */
  private static boolean isJUnit4AssumptionViolation(Throwable cause) {
    String className = cause.getClass().getName();
    return className.equals("org.junit.internal.AssumptionViolatedException")
        || className.equals("org.junit.AssumptionViolatedException");
  }

  public String getMessage() {
    return message;
  }

  public String getThrowableString() {
    return throwableString;
  }

  public TestDescriptionMirror getDescription() {
    return description;
  }

  public String getTrace() {
    return trace;
  }

  public boolean isAssumptionViolation() {
    return assumptionViolation;
  }

  public boolean isAssertionViolation() {
    return assertionViolation;
  }  

  public boolean isErrorViolation() {
    return isAssertionViolation() == false &&
           isAssumptionViolation() == false;
  }

  public String getThrowableClass() {
    return throwableClass;
  }
}

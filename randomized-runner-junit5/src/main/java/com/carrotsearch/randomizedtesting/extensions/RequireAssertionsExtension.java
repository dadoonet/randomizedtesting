package com.carrotsearch.randomizedtesting.extensions;

import java.util.Objects;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.SysGlobals;
import com.carrotsearch.randomizedtesting.annotations.SuppressForbidden;

/**
 * A JUnit 5 extension that verifies assertions are enabled/disabled as expected.
 * 
 * <p>Usage with {@literal @}RegisterExtension:
 * <pre>
 * {@literal @}RegisterExtension
 * static RequireAssertionsExtension assertions = new RequireAssertionsExtension(MyTest.class);
 * </pre>
 */
public class RequireAssertionsExtension implements BeforeAllCallback {
  
  public static final boolean TEST_ASSERTS_ENABLED = 
      RandomizedTest.systemPropertyAsBoolean(SysGlobals.SYSPROP_ASSERTS(), true);
  
  private final Class<?> targetClass;

  /**
   * Create extension that will check assertion status on the test class itself.
   */
  public RequireAssertionsExtension() {
    this.targetClass = null;
  }

  /**
   * Create extension that will check assertion status on a specific class.
   */
  public RequireAssertionsExtension(Class<?> targetClass) {
    this.targetClass = Objects.requireNonNull(targetClass);
  }

  @Override
  @SuppressForbidden("Permitted sysout.")
  public void beforeAll(ExtensionContext context) throws Exception {
    Class<?> classToCheck = targetClass != null ? targetClass : context.getRequiredTestClass();
    
    boolean assertsEnabled = classToCheck.desiredAssertionStatus();
    if (assertsEnabled != TEST_ASSERTS_ENABLED) {
      String msg = "Assertion state mismatch on " + classToCheck.getSimpleName() + ": ";
      if (assertsEnabled) {
        msg += "-ea was specified";
      } else {
        msg += "-ea was not specified";
      }
      msg += " but -Dtests.asserts=" + TEST_ASSERTS_ENABLED;

      System.err.println(msg);
      throw new Exception(msg);
    }
  }
}

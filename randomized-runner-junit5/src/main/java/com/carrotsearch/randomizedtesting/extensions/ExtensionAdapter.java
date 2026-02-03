package com.carrotsearch.randomizedtesting.extensions;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.*;

/**
 * An abstract extension adapter that provides a similar pattern to JUnit 4's TestRuleAdapter.
 * This guarantees the execution of {@link #afterAlways} even if an exception has been thrown.
 * 
 * <p>This can be used both at class level (BeforeAll/AfterAll) and test level (BeforeEach/AfterEach).
 * 
 * <p>Example:
 * <pre>
 * public class MyExtension extends ExtensionAdapter {
 *   {@literal @}Override
 *   protected void before(ExtensionContext context) throws Throwable {
 *     // setup
 *   }
 *   
 *   {@literal @}Override
 *   protected void afterAlways(ExtensionContext context, List&lt;Throwable&gt; errors) throws Throwable {
 *     // cleanup, always runs
 *   }
 * }
 * </pre>
 */
public abstract class ExtensionAdapter implements 
    BeforeAllCallback, AfterAllCallback, 
    BeforeEachCallback, AfterEachCallback {

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    try {
      beforeClass(context);
    } catch (Exception e) {
      throw e;
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    List<Throwable> errors = new ArrayList<>();
    try {
      afterClassAlways(context, errors);
    } catch (Throwable t) {
      errors.add(t);
    }
    throwIfErrors(errors);
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    try {
      before(context);
    } catch (Exception e) {
      throw e;
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    List<Throwable> errors = new ArrayList<>();
    try {
      afterAlways(context, errors);
    } catch (Throwable t) {
      errors.add(t);
    }
    
    try {
      afterIfSuccessful(context);
    } catch (Throwable t) {
      errors.add(t);
    }
    
    throwIfErrors(errors);
  }

  /**
   * Called before all tests in the class.
   */
  protected void beforeClass(ExtensionContext context) throws Throwable {}

  /**
   * Always called after all tests in the class, even if an exception occurs.
   */
  protected void afterClassAlways(ExtensionContext context, List<Throwable> errors) throws Throwable {}

  /**
   * Called before each test method.
   */
  protected void before(ExtensionContext context) throws Throwable {}
  
  /**
   * Always called after each test method, even if an exception occurs.
   * 
   * @param errors A list of errors received so far. The list is modifiable.
   */
  protected void afterAlways(ExtensionContext context, List<Throwable> errors) throws Throwable {}

  /**
   * Called only if the test method returned successfully.
   */
  protected void afterIfSuccessful(ExtensionContext context) throws Throwable {}

  private void throwIfErrors(List<Throwable> errors) throws Exception {
    if (errors.isEmpty()) {
      return;
    }
    if (errors.size() == 1) {
      Throwable t = errors.get(0);
      if (t instanceof Exception) {
        throw (Exception) t;
      }
      throw new RuntimeException(t);
    }
    StringBuilder sb = new StringBuilder("Multiple failures:\n");
    for (Throwable t : errors) {
      sb.append("  - ").append(t.getClass().getName()).append(": ").append(t.getMessage()).append("\n");
    }
    RuntimeException combined = new RuntimeException(sb.toString());
    for (Throwable t : errors) {
      combined.addSuppressed(t);
    }
    throw combined;
  }
}

package com.carrotsearch.randomizedtesting.extensions;

import java.lang.reflect.Method;
import java.util.concurrent.*;

import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

import com.carrotsearch.randomizedtesting.SysGlobals;
import com.carrotsearch.randomizedtesting.annotations.Timeout;
import com.carrotsearch.randomizedtesting.annotations.TimeoutSuite;

/**
 * JUnit 5 extension that provides timeout support for tests.
 * 
 * <p>This extension handles both method-level timeouts ({@link Timeout})
 * and suite-level timeouts ({@link TimeoutSuite}).
 * 
 * <p>Usage:
 * <pre>
 * {@literal @}ExtendWith(TimeoutExtension.class)
 * {@literal @}TimeoutSuite(millis = 60000)
 * public class MyTest {
 *   {@literal @}Test
 *   {@literal @}Timeout(millis = 1000)
 *   public void testWithTimeout() {
 *     // This test will fail if it takes more than 1 second
 *   }
 * }
 * </pre>
 * 
 * @see Timeout
 * @see TimeoutSuite
 */
public class TimeoutExtension implements InvocationInterceptor, BeforeAllCallback, AfterAllCallback {
  
  private static final Namespace NAMESPACE = Namespace.create(TimeoutExtension.class);
  private static final String KEY_SUITE_START = "suiteStart";
  private static final String KEY_SUITE_TIMEOUT = "suiteTimeout";
  
  /** Default timeout (disabled by default) */
  public static final int DEFAULT_TIMEOUT = 0;
  
  /** Default suite timeout (disabled by default) */
  public static final int DEFAULT_TIMEOUT_SUITE = 0;
  
  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    Class<?> testClass = context.getRequiredTestClass();
    Store store = context.getStore(NAMESPACE);
    
    // Record suite start time
    store.put(KEY_SUITE_START, System.currentTimeMillis());
    
    // Get suite timeout
    int suiteTimeout = getSuiteTimeout(testClass);
    store.put(KEY_SUITE_TIMEOUT, suiteTimeout);
  }
  
  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    // Check if suite timeout was exceeded
    Store store = context.getStore(NAMESPACE);
    Long startTime = store.get(KEY_SUITE_START, Long.class);
    Integer suiteTimeout = store.get(KEY_SUITE_TIMEOUT, Integer.class);
    
    if (startTime != null && suiteTimeout != null && suiteTimeout > 0) {
      long elapsed = System.currentTimeMillis() - startTime;
      if (elapsed > suiteTimeout) {
        throw new TimeoutException("Suite timeout exceeded (elapsed: " + elapsed + "ms, timeout: " + suiteTimeout + "ms)");
      }
    }
  }
  
  @Override
  public void interceptTestMethod(Invocation<Void> invocation,
                                  ReflectiveInvocationContext<Method> invocationContext,
                                  ExtensionContext extensionContext) throws Throwable {
    int timeout = getMethodTimeout(invocationContext.getExecutable(), extensionContext);
    
    if (timeout <= 0) {
      // No timeout, proceed normally
      invocation.proceed();
      return;
    }
    
    // Check suite timeout first
    checkSuiteTimeout(extensionContext);
    
    // Execute with timeout
    executeWithTimeout(invocation, timeout, "Test timeout exceeded");
  }
  
  @Override
  public void interceptBeforeEachMethod(Invocation<Void> invocation,
                                        ReflectiveInvocationContext<Method> invocationContext,
                                        ExtensionContext extensionContext) throws Throwable {
    int timeout = getMethodTimeout(invocationContext.getExecutable(), extensionContext);
    
    if (timeout <= 0) {
      invocation.proceed();
      return;
    }
    
    checkSuiteTimeout(extensionContext);
    executeWithTimeout(invocation, timeout, "BeforeEach timeout exceeded");
  }
  
  @Override
  public void interceptAfterEachMethod(Invocation<Void> invocation,
                                       ReflectiveInvocationContext<Method> invocationContext,
                                       ExtensionContext extensionContext) throws Throwable {
    int timeout = getMethodTimeout(invocationContext.getExecutable(), extensionContext);
    
    if (timeout <= 0) {
      invocation.proceed();
      return;
    }
    
    checkSuiteTimeout(extensionContext);
    executeWithTimeout(invocation, timeout, "AfterEach timeout exceeded");
  }
  
  private void checkSuiteTimeout(ExtensionContext context) throws TimeoutException {
    Store store = context.getStore(NAMESPACE);
    Long startTime = store.get(KEY_SUITE_START, Long.class);
    Integer suiteTimeout = store.get(KEY_SUITE_TIMEOUT, Integer.class);
    
    if (startTime != null && suiteTimeout != null && suiteTimeout > 0) {
      long elapsed = System.currentTimeMillis() - startTime;
      if (elapsed > suiteTimeout) {
        throw new TimeoutException("Suite timeout exceeded (elapsed: " + elapsed + "ms, timeout: " + suiteTimeout + "ms)");
      }
    }
  }
  
  private void executeWithTimeout(Invocation<Void> invocation, int timeoutMillis, String message) throws Throwable {
    ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "TimeoutExtension-worker");
      t.setDaemon(true);
      return t;
    });
    
    Future<Void> future = executor.submit(() -> {
      try {
        invocation.proceed();
        return null;
      } catch (Throwable t) {
        throw new ExecutionException(t);
      }
    });
    
    try {
      future.get(timeoutMillis, TimeUnit.MILLISECONDS);
    } catch (java.util.concurrent.TimeoutException e) {
      future.cancel(true);
      throw new TimeoutException(message + " (timeout: " + timeoutMillis + "ms)");
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof ExecutionException && cause.getCause() != null) {
        throw cause.getCause();
      }
      throw cause != null ? cause : e;
    } finally {
      executor.shutdownNow();
    }
  }
  
  private int getMethodTimeout(Method method, ExtensionContext context) {
    // Check method-level @Timeout annotation
    Timeout methodTimeout = method.getAnnotation(Timeout.class);
    if (methodTimeout != null) {
      return methodTimeout.millis();
    }
    
    // Check class-level @Timeout annotation
    Class<?> testClass = context.getRequiredTestClass();
    Timeout classTimeout = testClass.getAnnotation(Timeout.class);
    if (classTimeout != null) {
      return classTimeout.millis();
    }
    
    // Check system property
    String sysProp = System.getProperty(SysGlobals.SYSPROP_TIMEOUT());
    if (sysProp != null && !sysProp.isEmpty()) {
      try {
        return Integer.parseInt(sysProp);
      } catch (NumberFormatException e) {
        // ignore
      }
    }
    
    return DEFAULT_TIMEOUT;
  }
  
  private int getSuiteTimeout(Class<?> testClass) {
    // Check @TimeoutSuite annotation
    TimeoutSuite suiteTimeout = testClass.getAnnotation(TimeoutSuite.class);
    if (suiteTimeout != null) {
      return suiteTimeout.millis();
    }
    
    // Check system property
    String sysProp = System.getProperty(SysGlobals.SYSPROP_TIMEOUT_SUITE());
    if (sysProp != null && !sysProp.isEmpty()) {
      try {
        return Integer.parseInt(sysProp);
      } catch (NumberFormatException e) {
        // ignore
      }
    }
    
    return DEFAULT_TIMEOUT_SUITE;
  }
  
  /**
   * Exception thrown when a timeout is exceeded.
   */
  public static class TimeoutException extends RuntimeException {
    public TimeoutException(String message) {
      super(message);
    }
  }
}

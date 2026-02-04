package com.carrotsearch.randomizedtesting.extensions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.TestAbortedException;

import com.carrotsearch.randomizedtesting.annotations.Listeners;
import com.carrotsearch.randomizedtesting.listeners.RandomizedTestListener;
import com.carrotsearch.randomizedtesting.listeners.TestInfo;

/**
 * JUnit 5 extension that supports the {@link Listeners} annotation.
 * Instantiates and invokes registered listeners at appropriate lifecycle points.
 */
public class ListenersExtension implements 
    BeforeAllCallback, 
    AfterAllCallback,
    BeforeEachCallback, 
    AfterEachCallback,
    TestExecutionExceptionHandler {

  private static final Namespace NAMESPACE = Namespace.create(ListenersExtension.class);
  private static final String KEY_LISTENERS = "listeners";
  private static final String KEY_TEST_FAILURE = "testFailure";
  private static final String KEY_TEST_ABORTED = "testAborted";
  private static final String KEY_RUN_COUNT = "runCount";

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    List<RandomizedTestListener> listeners = createListeners(context);
    getClassStore(context).put(KEY_LISTENERS, listeners);
    getClassStore(context).put(KEY_RUN_COUNT, 0);
    
    for (RandomizedTestListener listener : listeners) {
      listener.testRunStarted();
    }
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    List<RandomizedTestListener> listeners = getListeners(context);
    if (listeners != null) {
      Integer runCount = getClassStore(context).get(KEY_RUN_COUNT, Integer.class);
      for (RandomizedTestListener listener : listeners) {
        listener.testRunFinished(runCount != null ? runCount : 0);
      }
    }
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    // Clear any previous test state
    getMethodStore(context).remove(KEY_TEST_FAILURE);
    getMethodStore(context).remove(KEY_TEST_ABORTED);
    
    // Increment run count
    Integer runCount = getClassStore(context).get(KEY_RUN_COUNT, Integer.class);
    getClassStore(context).put(KEY_RUN_COUNT, (runCount != null ? runCount : 0) + 1);
    
    List<RandomizedTestListener> listeners = getListeners(context);
    if (listeners != null) {
      TestInfo testInfo = createTestInfo(context);
      for (RandomizedTestListener listener : listeners) {
        listener.testStarted(testInfo);
      }
    }
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    List<RandomizedTestListener> listeners = getListeners(context);
    if (listeners == null) {
      return;
    }
    
    TestInfo testInfo = createTestInfo(context);
    Throwable failure = getMethodStore(context).get(KEY_TEST_FAILURE, Throwable.class);
    Throwable aborted = getMethodStore(context).get(KEY_TEST_ABORTED, Throwable.class);
    
    for (RandomizedTestListener listener : listeners) {
      if (failure != null) {
        listener.testFailed(testInfo, failure);
      } else if (aborted != null) {
        listener.testAborted(testInfo, aborted);
      } else {
        listener.testFinished(testInfo);
      }
    }
  }

  @Override
  public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
    if (throwable instanceof TestAbortedException) {
      getMethodStore(context).put(KEY_TEST_ABORTED, throwable);
    } else {
      getMethodStore(context).put(KEY_TEST_FAILURE, throwable);
    }
    // Re-throw to let JUnit handle the exception
    throw throwable;
  }

  @SuppressWarnings("unchecked")
  private List<RandomizedTestListener> getListeners(ExtensionContext context) {
    // Walk up to find the class-level store
    ExtensionContext current = context;
    while (current != null) {
      List<RandomizedTestListener> listeners = current.getStore(NAMESPACE).get(KEY_LISTENERS, List.class);
      if (listeners != null) {
        return listeners;
      }
      current = current.getParent().orElse(null);
    }
    return null;
  }

  private List<RandomizedTestListener> createListeners(ExtensionContext context) throws Exception {
    List<RandomizedTestListener> listeners = new ArrayList<>();
    
    // Walk the class hierarchy to collect all @Listeners annotations
    Class<?> testClass = context.getRequiredTestClass();
    collectListeners(testClass, listeners);
    
    return listeners;
  }

  private void collectListeners(Class<?> clazz, List<RandomizedTestListener> listeners) throws Exception {
    if (clazz == null || clazz == Object.class) {
      return;
    }
    
    // Process parent first (so parent listeners fire first)
    collectListeners(clazz.getSuperclass(), listeners);
    
    Listeners annotation = clazz.getDeclaredAnnotation(Listeners.class);
    if (annotation != null) {
      for (Class<? extends RandomizedTestListener> listenerClass : annotation.value()) {
        listeners.add(listenerClass.getDeclaredConstructor().newInstance());
      }
    }
  }

  private ExtensionContext.Store getClassStore(ExtensionContext context) {
    return context.getStore(NAMESPACE);
  }

  private ExtensionContext.Store getMethodStore(ExtensionContext context) {
    return context.getStore(Namespace.create(NAMESPACE, context.getUniqueId()));
  }

  /**
   * Creates a TestInfo from the current ExtensionContext.
   */
  private TestInfo createTestInfo(ExtensionContext context) {
    return new SimpleTestInfo(context);
  }

  /**
   * A simple implementation of TestInfo that wraps an ExtensionContext.
   */
  private static class SimpleTestInfo implements TestInfo {
    private final String displayName;
    private final String uniqueId;
    private final Class<?> testClass;
    private final Method testMethod;
    
    SimpleTestInfo(ExtensionContext context) {
      this.displayName = context.getDisplayName();
      this.uniqueId = context.getUniqueId();
      this.testClass = context.getTestClass().orElse(null);
      this.testMethod = context.getTestMethod().orElse(null);
    }
    
    @Override
    public String getDisplayName() {
      return displayName;
    }
    
    @Override
    public String getUniqueId() {
      return uniqueId;
    }
    
    @Override
    public Optional<Class<?>> getTestClass() {
      return Optional.ofNullable(testClass);
    }
    
    @Override
    public Optional<Method> getTestMethod() {
      return Optional.ofNullable(testMethod);
    }
    
    @Override
    public boolean isTest() {
      return testMethod != null;
    }
  }
}

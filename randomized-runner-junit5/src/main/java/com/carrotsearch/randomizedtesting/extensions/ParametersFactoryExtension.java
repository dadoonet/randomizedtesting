package com.carrotsearch.randomizedtesting.extensions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.support.AnnotationSupport;
import org.opentest4j.TestAbortedException;

import com.carrotsearch.randomizedtesting.Randomness;
import com.carrotsearch.randomizedtesting.RandomizedContext;
import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import com.carrotsearch.randomizedtesting.annotations.TestCaseInstanceProvider;

/**
 * JUnit 5 extension that provides support for {@link ParametersFactory} annotation.
 * 
 * <p>This extension allows creating parameterized test classes where parameters
 * are passed to the constructor. Each parameter set creates a new test instance.
 * 
 * <p>Usage:
 * <pre>
 * {@literal @}ExtendWith({RandomizedExtension.class, ParametersFactoryExtension.class})
 * public class MyParameterizedTest {
 *   private final int value;
 *   
 *   public MyParameterizedTest({@literal @}Name("value") int value) {
 *     this.value = value;
 *   }
 *   
 *   {@literal @}ParametersFactory
 *   public static Iterable&lt;Object[]&gt; parameters() {
 *     return Arrays.asList(new Object[]{1}, new Object[]{2}, new Object[]{3});
 *   }
 *   
 *   {@literal @}Test
 *   public void testSomething() {
 *     // Test using this.value
 *   }
 * }
 * </pre>
 * 
 * @see ParametersFactory
 * @see Name
 */
public class ParametersFactoryExtension implements TestInstanceFactory {
  
  private static final ExtensionContext.Namespace NAMESPACE = 
      ExtensionContext.Namespace.create(ParametersFactoryExtension.class);
  private static final String KEY_PARAMETERS = "parameters";
  private static final String KEY_CURRENT_INDEX = "currentIndex";
  private static final String KEY_CACHED_INSTANCES = "cachedInstances";
  
  @Override
  public Object createTestInstance(TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext)
      throws TestInstantiationException {
    Class<?> testClass = factoryContext.getTestClass();
    
    // Check if we should reuse instances per constructor args
    boolean reuseInstances = shouldReuseInstances(testClass);
    
    // Find @ParametersFactory method
    Method factoryMethod = findFactoryMethod(testClass);
    if (factoryMethod == null) {
      // No factory method - use default constructor
      if (reuseInstances) {
        return getOrCreateCachedInstance(testClass, new Object[0], extensionContext);
      }
      return createDefaultInstance(testClass);
    }
    
    // Get or create parameters list
    List<Object[]> parameters = getOrCreateParameters(factoryMethod, extensionContext);
    if (parameters.isEmpty()) {
      throw new TestAbortedException("No parameters provided by @ParametersFactory");
    }
    
    // Get current parameter index
    ExtensionContext.Store store = extensionContext.getStore(NAMESPACE);
    Integer currentIndex = store.get(KEY_CURRENT_INDEX, Integer.class);
    if (currentIndex == null) {
      currentIndex = 0;
    }
    
    // Wrap around if needed (for multiple test methods)
    int index = currentIndex % parameters.size();
    Object[] params = parameters.get(index);
    
    // Increment index for next test
    store.put(KEY_CURRENT_INDEX, currentIndex + 1);
    
    // Create or retrieve cached instance with parameters
    if (reuseInstances) {
      return getOrCreateCachedInstance(testClass, params, extensionContext);
    }
    return createInstanceWithParameters(testClass, params);
  }
  
  /**
   * Check if the test class should reuse instances per constructor args.
   */
  private boolean shouldReuseInstances(Class<?> testClass) {
    // Check in class hierarchy for @TestCaseInstanceProvider
    Class<?> clazz = testClass;
    while (clazz != null && clazz != Object.class) {
      TestCaseInstanceProvider annotation = clazz.getAnnotation(TestCaseInstanceProvider.class);
      if (annotation != null) {
        return annotation.value() == TestCaseInstanceProvider.Type.INSTANCE_PER_CONSTRUCTOR_ARGS;
      }
      clazz = clazz.getSuperclass();
    }
    return false;
  }
  
  /**
   * Get or create a cached instance for the given constructor arguments.
   */
  @SuppressWarnings("unchecked")
  private Object getOrCreateCachedInstance(Class<?> testClass, Object[] params, ExtensionContext context) {
    ExtensionContext.Store store = context.getRoot().getStore(NAMESPACE);
    String classKey = KEY_CACHED_INSTANCES + ":" + testClass.getName();
    
    Map<String, Object> instanceCache = (Map<String, Object>) store.get(classKey, Map.class);
    if (instanceCache == null) {
      instanceCache = new HashMap<>();
      store.put(classKey, instanceCache);
    }
    
    String paramsKey = Arrays.deepToString(params);
    Object instance = instanceCache.get(paramsKey);
    if (instance == null) {
      instance = createInstanceWithParameters(testClass, params);
      instanceCache.put(paramsKey, instance);
    }
    return instance;
  }
  
  private Method findFactoryMethod(Class<?> testClass) {
    // Search in class hierarchy
    Class<?> clazz = testClass;
    while (clazz != null && clazz != Object.class) {
      for (Method method : clazz.getDeclaredMethods()) {
        if (method.isAnnotationPresent(ParametersFactory.class)) {
          validateFactoryMethod(method);
          return method;
        }
      }
      clazz = clazz.getSuperclass();
    }
    return null;
  }
  
  private void validateFactoryMethod(Method method) {
    if (!Modifier.isStatic(method.getModifiers())) {
      throw new IllegalArgumentException("@ParametersFactory method must be static: " + method);
    }
    if (!Modifier.isPublic(method.getModifiers())) {
      throw new IllegalArgumentException("@ParametersFactory method must be public: " + method);
    }
    if (method.getParameterCount() != 0) {
      throw new IllegalArgumentException("@ParametersFactory method must have no parameters: " + method);
    }
    if (!Iterable.class.isAssignableFrom(method.getReturnType())) {
      throw new IllegalArgumentException("@ParametersFactory method must return Iterable<Object[]>: " + method);
    }
  }
  
  @SuppressWarnings("unchecked")
  private List<Object[]> getOrCreateParameters(Method factoryMethod, ExtensionContext context) {
    ExtensionContext.Store store = context.getRoot().getStore(NAMESPACE);
    String key = KEY_PARAMETERS + ":" + factoryMethod.getDeclaringClass().getName();
    
    List<Object[]> parameters = (List<Object[]>) store.get(key, List.class);
    if (parameters == null) {
      parameters = invokeFactoryMethod(factoryMethod);
      store.put(key, parameters);
    }
    return parameters;
  }
  
  @SuppressWarnings("unchecked")
  private List<Object[]> invokeFactoryMethod(Method method) {
    try {
      method.setAccessible(true);
      Iterable<Object[]> iterable = (Iterable<Object[]>) method.invoke(null);
      
      List<Object[]> result = new ArrayList<>();
      for (Object[] params : iterable) {
        // Handle non-Object arrays (e.g., Integer[])
        if (params != null && params.getClass() != Object[].class) {
          Object[] converted = new Object[params.length];
          System.arraycopy(params, 0, converted, 0, params.length);
          result.add(converted);
        } else {
          result.add(params);
        }
      }
      
      // Shuffle if requested
      ParametersFactory annotation = method.getAnnotation(ParametersFactory.class);
      if (annotation.shuffle() && !result.isEmpty()) {
        try {
          Randomness randomness = RandomizedContext.current().getRandomness();
          Collections.shuffle(result, randomness.getRandom());
        } catch (IllegalStateException e) {
          // No randomized context available, skip shuffling
        }
      }
      
      return result;
    } catch (TestAbortedException e) {
      // Assumption failed in factory method
      return Collections.emptyList();
    } catch (Exception e) {
      if (e.getCause() instanceof TestAbortedException) {
        return Collections.emptyList();
      }
      throw new RuntimeException("Failed to invoke @ParametersFactory method: " + method, e);
    }
  }
  
  private Object createDefaultInstance(Class<?> testClass) {
    try {
      Constructor<?> constructor = testClass.getDeclaredConstructor();
      constructor.setAccessible(true);
      return constructor.newInstance();
    } catch (Exception e) {
      throw new TestInstantiationException("Failed to create test instance", e);
    }
  }
  
  private Object createInstanceWithParameters(Class<?> testClass, Object[] params) {
    try {
      // Find constructor matching parameter count
      Constructor<?> matchingConstructor = null;
      for (Constructor<?> constructor : testClass.getDeclaredConstructors()) {
        if (constructor.getParameterCount() == params.length) {
          matchingConstructor = constructor;
          break;
        }
      }
      
      if (matchingConstructor == null) {
        // Try default constructor if params is empty
        if (params.length == 0) {
          return createDefaultInstance(testClass);
        }
        throw new TestInstantiationException(
            "No constructor found with " + params.length + " parameters in " + testClass);
      }
      
      matchingConstructor.setAccessible(true);
      return matchingConstructor.newInstance(params);
    } catch (TestInstantiationException e) {
      throw e;
    } catch (Exception e) {
      throw new TestInstantiationException(
          "Failed to create test instance with parameters: " + Arrays.toString(params), e);
    }
  }
  
  /**
   * Format parameters for display name.
   */
  public static String formatParameters(Object[] params, Method factoryMethod, Constructor<?> constructor) {
    if (params == null || params.length == 0) {
      return "";
    }
    
    ParametersFactory annotation = factoryMethod.getAnnotation(ParametersFactory.class);
    String format = annotation != null ? annotation.argumentFormatting() : ParametersFactory.DEFAULT_FORMATTING;
    
    if (ParametersFactory.DEFAULT_FORMATTING.equals(format)) {
      // Use @Name annotations if available
      StringBuilder sb = new StringBuilder();
      Parameter[] parameters = constructor.getParameters();
      for (int i = 0; i < params.length; i++) {
        if (i > 0) sb.append(", ");
        String name = null;
        if (i < parameters.length) {
          Name nameAnn = parameters[i].getAnnotation(Name.class);
          if (nameAnn != null) {
            name = nameAnn.value();
          }
        }
        if (name != null) {
          sb.append(name).append("=");
        }
        sb.append(params[i]);
      }
      return sb.toString();
    } else {
      // Use custom formatting
      try {
        return String.format(Locale.ROOT, format, params);
      } catch (Exception e) {
        return Arrays.toString(params);
      }
    }
  }
}

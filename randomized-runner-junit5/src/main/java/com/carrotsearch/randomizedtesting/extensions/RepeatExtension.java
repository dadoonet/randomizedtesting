package com.carrotsearch.randomizedtesting.extensions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import com.carrotsearch.randomizedtesting.annotations.Repeat;

/**
 * JUnit 5 extension that provides support for the {@link Repeat} annotation.
 * 
 * <p>This extension works with {@code @TestTemplate} annotated methods to repeat
 * test execution multiple times.
 * 
 * <p>Usage:
 * <pre>
 * {@literal @}ExtendWith(RepeatExtension.class)
 * public class MyTest {
 *   {@literal @}TestTemplate
 *   {@literal @}Repeat(iterations = 5)
 *   public void repeatedTest() {
 *     // This test will run 5 times
 *   }
 * }
 * </pre>
 * 
 * <p>Note: For simple repetition, consider using JUnit 5's built-in
 * {@code @RepeatedTest} annotation instead.
 * 
 * @see Repeat
 */
public class RepeatExtension implements TestTemplateInvocationContextProvider {

  @Override
  public boolean supportsTestTemplate(ExtensionContext context) {
    return context.getTestMethod()
        .map(method -> method.isAnnotationPresent(Repeat.class))
        .orElse(false);
  }

  @Override
  public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
      ExtensionContext context) {
    
    Method method = context.getRequiredTestMethod();
    Repeat repeat = method.getAnnotation(Repeat.class);
    
    int iterations = repeat.iterations();
    if (iterations < 1) {
      iterations = 1;
    }
    
    boolean useConstantSeed = repeat.useConstantSeed();
    
    List<TestTemplateInvocationContext> contexts = new ArrayList<>();
    for (int i = 0; i < iterations; i++) {
      final int iteration = i + 1;
      contexts.add(new RepeatInvocationContext(iteration, iterations, useConstantSeed));
    }
    
    return contexts.stream();
  }
  
  /**
   * Invocation context for a single repetition.
   */
  private static class RepeatInvocationContext implements TestTemplateInvocationContext {
    private final int currentIteration;
    private final int totalIterations;
    private final boolean useConstantSeed;
    
    RepeatInvocationContext(int currentIteration, int totalIterations, boolean useConstantSeed) {
      this.currentIteration = currentIteration;
      this.totalIterations = totalIterations;
      this.useConstantSeed = useConstantSeed;
    }
    
    @Override
    public String getDisplayName(int invocationIndex) {
      return String.format(Locale.ROOT, "repetition %d of %d", currentIteration, totalIterations);
    }
  }
}

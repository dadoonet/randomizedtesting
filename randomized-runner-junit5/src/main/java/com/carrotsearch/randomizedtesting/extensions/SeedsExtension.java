package com.carrotsearch.randomizedtesting.extensions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import com.carrotsearch.randomizedtesting.SeedUtils;
import com.carrotsearch.randomizedtesting.annotations.Seed;
import com.carrotsearch.randomizedtesting.annotations.Seeds;

/**
 * JUnit 5 extension that provides support for {@link Seeds} annotation.
 * 
 * <p>This extension allows running a test method multiple times with different
 * seeds. Each {@link Seed} in the {@link Seeds} annotation causes one test
 * invocation.
 * 
 * <p>Usage with {@code @TestTemplate}:
 * <pre>
 * {@literal @}ExtendWith({RandomizedExtension.class, SeedsExtension.class})
 * public class MyTest extends RandomizedTest {
 *   {@literal @}TestTemplate
 *   {@literal @}Seeds({
 *     {@literal @}Seed("deadbeef"),
 *     {@literal @}Seed("cafebabe"),
 *     {@literal @}Seed // random seed
 *   })
 *   public void testWithMultipleSeeds() {
 *     // Test runs 3 times with different seeds
 *   }
 * }
 * </pre>
 * 
 * @see Seeds
 * @see Seed
 */
public class SeedsExtension implements TestTemplateInvocationContextProvider {
  
  private static final ExtensionContext.Namespace NAMESPACE = 
      ExtensionContext.Namespace.create(SeedsExtension.class);
  private static final String KEY_SEED = "currentSeed";
  
  /**
   * Thread-local to pass the seed from SeedsExtension to RandomizedExtension.
   * This is needed because the order of extension execution can vary.
   */
  private static final ThreadLocal<Seed> CURRENT_SEED = new ThreadLocal<>();
  
  @Override
  public boolean supportsTestTemplate(ExtensionContext context) {
    return context.getTestMethod()
        .map(method -> method.isAnnotationPresent(Seeds.class))
        .orElse(false);
  }
  
  @Override
  public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
    Method method = context.getRequiredTestMethod();
    Seeds seeds = method.getAnnotation(Seeds.class);
    
    if (seeds == null || seeds.value().length == 0) {
      return Stream.empty();
    }
    
    List<TestTemplateInvocationContext> contexts = new ArrayList<>();
    for (int i = 0; i < seeds.value().length; i++) {
      Seed seed = seeds.value()[i];
      contexts.add(new SeedInvocationContext(seed, i + 1, seeds.value().length));
    }
    
    return contexts.stream();
  }
  
  /**
   * Parse a seed value from the annotation.
   * 
   * @param seedValue the seed string ("random" or hex value)
   * @param fallbackSeed seed to use if "random"
   * @return the parsed seed value
   */
  public static long parseSeed(String seedValue, long fallbackSeed) {
    if (seedValue == null || seedValue.isEmpty() || "random".equalsIgnoreCase(seedValue)) {
      return fallbackSeed;
    }
    return SeedUtils.parseSeedChain(seedValue)[0];
  }
  
  /**
   * Check if a seed value represents a random seed.
   */
  public static boolean isRandomSeed(String seedValue) {
    return seedValue == null || seedValue.isEmpty() || "random".equalsIgnoreCase(seedValue);
  }
  
  /**
   * Invocation context for a specific seed.
   */
  private static class SeedInvocationContext implements TestTemplateInvocationContext {
    private final Seed seed;
    private final int index;
    private final int total;
    
    SeedInvocationContext(Seed seed, int index, int total) {
      this.seed = seed;
      this.index = index;
      this.total = total;
    }
    
    @Override
    public String getDisplayName(int invocationIndex) {
      String seedValue = seed.value();
      if (isRandomSeed(seedValue)) {
        return String.format(Locale.ROOT, "[%d/%d] seed=random", index, total);
      } else {
        return String.format(Locale.ROOT, "[%d/%d] seed=%s", index, total, seedValue);
      }
    }
    
    @Override
    public List<Extension> getAdditionalExtensions() {
      // Set the seed in thread-local BEFORE returning extensions
      // This ensures it's available when RandomizedExtension.beforeEach runs
      CURRENT_SEED.set(seed);
      return Collections.singletonList(new SeedSetupExtension(seed));
    }
  }
  
  /**
   * Extension that sets up the seed before each test invocation.
   */
  private static class SeedSetupExtension implements BeforeEachCallback {
    private final Seed seed;
    
    SeedSetupExtension(Seed seed) {
      this.seed = seed;
    }
    
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
      // Store the seed in thread-local for RandomizedExtension to use
      CURRENT_SEED.set(seed);
      // Also store in context for cleanup
      ExtensionContext.Store store = context.getStore(NAMESPACE);
      store.put(KEY_SEED, seed);
    }
  }
  
  /**
   * Get the seed for the current test.
   * Returns null if no seed is set.
   */
  public static Seed getCurrentSeed(ExtensionContext context) {
    // First check thread-local (set by SeedsExtension)
    Seed seed = CURRENT_SEED.get();
    if (seed != null) {
      return seed;
    }
    // Fall back to context store
    ExtensionContext.Store store = context.getStore(NAMESPACE);
    return store.get(KEY_SEED, Seed.class);
  }
  
  /**
   * Clear the current seed (should be called after test execution).
   */
  public static void clearCurrentSeed() {
    CURRENT_SEED.remove();
  }
}

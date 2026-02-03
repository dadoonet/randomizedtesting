package com.carrotsearch.randomizedtesting.extensions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * A JUnit 5 extension that prevents {@link BeforeEach} and {@link AfterEach} hook overrides 
 * as it is most likely a user error and will result in superclass methods not being called
 * (requires manual chaining).
 * 
 * <p>Usage:
 * <pre>
 * {@literal @}ExtendWith(NoInstanceHooksOverridesExtension.class)
 * public class MyTest {
 *   // tests...
 * }
 * </pre>
 */
public class NoInstanceHooksOverridesExtension extends NoShadowingOrOverridesExtension {
  
  public NoInstanceHooksOverridesExtension() {
    super(BeforeEach.class, AfterEach.class);
  }
}

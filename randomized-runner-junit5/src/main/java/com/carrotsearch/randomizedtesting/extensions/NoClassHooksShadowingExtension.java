package com.carrotsearch.randomizedtesting.extensions;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * A JUnit 5 extension that prevents {@link BeforeAll} and {@link AfterAll} hook shadowing 
 * as it is most likely a user error. JUnit rules for shadowed hook methods are weird.
 * 
 * <p>Usage:
 * <pre>
 * {@literal @}ExtendWith(NoClassHooksShadowingExtension.class)
 * public class MyTest {
 *   // tests...
 * }
 * </pre>
 */
public class NoClassHooksShadowingExtension extends NoShadowingOrOverridesExtension {
  
  public NoClassHooksShadowingExtension() {
    super(BeforeAll.class, AfterAll.class);
  }
}

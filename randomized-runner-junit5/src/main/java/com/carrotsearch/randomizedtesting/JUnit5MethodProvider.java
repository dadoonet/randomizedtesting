package com.carrotsearch.randomizedtesting;

import org.junit.jupiter.api.Test;

/**
 * Method provider selecting {@link Test} annotated public instance parameterless methods.
 */
public class JUnit5MethodProvider extends AnnotatedMethodProvider {
  public JUnit5MethodProvider() {
    super(Test.class);
  }
}

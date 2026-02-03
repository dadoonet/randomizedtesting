package com.carrotsearch.randomizedtesting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;

/**
 * Lifecycle stages for tracking resources.
 */
public enum LifecycleScope {
  /**
   * A single test case, including all {@link AfterEach} hooks.
   */
  TEST,
  
  /**
   * A single suite (class), including all {@link AfterAll} hooks.
   */
  SUITE;
}

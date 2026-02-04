package com.carrotsearch.randomizedtesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Constants and shared utilities for the randomized testing framework.
 */
public final class RandomizedRunnerConstants {
  
  private RandomizedRunnerConstants() {}

  /**
   * Package name used for augmented seed info.
   * This must match RandomizedExtension.AUGMENTED_SEED_PACKAGE.
   */
  public static final String AUGMENTED_SEED_PACKAGE = "__randomizedtesting";

  /**
   * Shared logger for the randomized testing framework.
   */
  public static final Logger logger = Logger.getLogger(RandomizedRunnerConstants.class.getPackage().getName());

  /**
   * Default timeout for a single test case (in milliseconds).
   * 0 means no timeout.
   */
  public static final int DEFAULT_TIMEOUT = 0;

  /**
   * Default timeout for an entire test suite (in milliseconds).
   * 0 means no timeout.
   */
  public static final int DEFAULT_TIMEOUT_SUITE = 0;

  /**
   * Default number of attempts to interrupt and kill a runaway thread.
   */
  public static final int DEFAULT_KILLATTEMPTS = 5;

  /**
   * Default wait time between kill attempts (in milliseconds).
   */
  public static final int DEFAULT_KILLWAIT = 1000;

  /**
   * The "main" thread group. We expect all test threads will be somewhere under it.
   */
  public static final ThreadGroup mainThreadGroup;

  static {
    ThreadGroup tg = Thread.currentThread().getThreadGroup();
    while (tg.getParent() != null && !"main".equals(tg.getName())) {
      tg = tg.getParent();
    }
    mainThreadGroup = tg;
  }

  /**
   * Convert empty string to null.
   */
  public static String emptyToNull(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    return value;
  }

  /**
   * Augments the stack trace of the given exception with seed information.
   */
  public static <T extends Throwable> T augmentStackTrace(T e) {
    Randomness[] seeds;
    try {
      seeds = RandomizedContext.current().getRandomnesses();
    } catch (IllegalStateException ex) {
      // No context available, just return the exception as-is
      return e;
    }
    return augmentStackTrace(e, seeds);
  }

  /**
   * Augments the stack trace of the given exception with seed information.
   */
  public static <T extends Throwable> T augmentStackTrace(T e, Randomness... seeds) {
    final String seedChain = SeedUtils.formatSeedChain(seeds);
    final String existingSeed = seedFromThrowable(e);  
    if (existingSeed != null && existingSeed.equals(seedChain)) {
      return e;
    }

    List<StackTraceElement> stack = new ArrayList<>(Arrays.asList(e.getStackTrace()));
    stack.add(0, new StackTraceElement(AUGMENTED_SEED_PACKAGE + ".SeedInfo", 
        "seed", seedChain, 0));

    e.setStackTrace(stack.toArray(new StackTraceElement[0]));
    return e;
  }

  /**
   * Extract seed information from a throwable's stack trace, if present.
   * This handles chained exceptions.
   */
  public static String seedFromThrowable(Throwable t) {
    StringBuilder b = new StringBuilder();
    while (t != null) {
      for (StackTraceElement ste : t.getStackTrace()) {
        if (ste.getClassName().startsWith(AUGMENTED_SEED_PACKAGE)) {
          if (b.length() > 0) b.append(", ");
          b.append(ste.getFileName());
        }
      }
      t = t.getCause();
    }
    return b.length() > 0 ? b.toString() : null;
  }
}

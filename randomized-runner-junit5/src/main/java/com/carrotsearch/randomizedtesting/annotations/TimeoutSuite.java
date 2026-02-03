package com.carrotsearch.randomizedtesting.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.Extension;

import com.carrotsearch.randomizedtesting.RandomizedExtension;
import com.carrotsearch.randomizedtesting.SysGlobals;

/**
 * Maximum execution time for an entire suite (including all hooks and tests).
 * Suite is defined as any class-scope {@link Extension}s, {@link BeforeAll}
 * and {@link AfterAll} hooks, suite class's constructor, instance-scope
 * {@link Extension}s, {@link BeforeEach} and {@link AfterEach} hooks and {@link Test}
 * methods.
 * 
 * <p>
 * The suite class's static initializer is <b>not</b> part of the measured code
 * (if you have static initializers in your tests, get rid of them).
 * 
 * <p>
 * Overrides the global default {@link RandomizedExtension#DEFAULT_TIMEOUT} or a
 * system property override {@link SysGlobals#SYSPROP_TIMEOUT}.
 * 
 * @see Timeout
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface TimeoutSuite {
  /**
   * Timeout time in millis. The timeout time is approximate, it may take longer
   * to actually abort the suite.
   */
  public int millis();
}

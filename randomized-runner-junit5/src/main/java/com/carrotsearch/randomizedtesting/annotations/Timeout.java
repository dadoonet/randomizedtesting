package com.carrotsearch.randomizedtesting.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.Extension;

import com.carrotsearch.randomizedtesting.RandomizedExtension;
import com.carrotsearch.randomizedtesting.SysGlobals;

/**
 * Maximum execution time for a single test method. Test methods are defined as
 * any instance-scope {@link Extension}s, {@link BeforeEach} and {@link AfterEach} hooks
 * and {@link Test} methods. Suite class's constructor is <b>not</b> part of the
 * measured code (see {@link TimeoutSuite}).
 * 
 * <p>
 * Overrides a global default {@link RandomizedExtension#DEFAULT_TIMEOUT} or a
 * system property override {@link SysGlobals#SYSPROP_TIMEOUT}.
 * 
 * @see TimeoutSuite
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface Timeout {
  /**
   * Timeout time in millis. The timeout time is approximate, it may take longer
   * to actually abort the test case.
   */
  public int millis();
}

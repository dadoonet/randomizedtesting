package com.carrotsearch.randomizedtesting.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.carrotsearch.randomizedtesting.RandomizedExtension;
import com.carrotsearch.randomizedtesting.listeners.RandomizedTestListener;

/**
 * Annotate your suite class with this annotation to automatically add hooks to
 * the test execution inside {@link RandomizedExtension}.
 * 
 * @see #value() 
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Listeners {
  /**
   * An array of listener classes. These classes must be instantiable (public, static, no-args
   * constructor, etc.).
   */
  Class<? extends RandomizedTestListener>[] value();
}

package com.carrotsearch.randomizedtesting.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.RegisterExtension;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface ThreadLeakScope {
  public static enum Scope {
    /**
     * No thread leaks from any individual test (including extensions registered 
     * via {@link RegisterExtension}) or the entire suite.
     */
    TEST,

    /**
     * No thread leaks from entire suite scope (individual tests may leak threads,
     * they become part of the suite scope).  
     */
    SUITE, 

    /** 
     * No thread leak checks at all. Highly discouraged.
     */
    NONE
  }
  
  Scope value() default Scope.TEST;
}
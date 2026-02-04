package com.carrotsearch.randomizedtesting.timeouts;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.RandomizedExtension;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.WithNestedTestClass;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope.Scope;
import com.carrotsearch.randomizedtesting.annotations.TimeoutSuite;

/**
 * Checks that stack traces are logged when threads are leaked during a timeout.
 */
public class Test018TimeoutStacks extends WithNestedTestClass {
  
  @ExtendWith(RandomizedExtension.class)
  @ThreadLeakScope(Scope.TEST)
  @TimeoutSuite(millis = 1000)
  public static class Nested1 extends RandomizedTest {
    @Test
    public void testFooBars() throws Exception {
      assumeRunningNested();
      for (int i = 0; i < 5; i++) {
        startThread("foobar-" + i);
      }
      Thread.sleep(5000);
    }
  }

  @Test
  public void testExceptionInFilter() throws Throwable {
    runTests(Nested1.class);
    Assertions.assertThat(getLoggingMessages()).contains("sleepForever(");
  }    
}

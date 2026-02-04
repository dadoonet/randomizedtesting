package com.carrotsearch.randomizedtesting;

import static com.carrotsearch.randomizedtesting.annotations.TestGroup.Utilities.getSysProperty;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.carrotsearch.randomizedtesting.annotations.TestGroup;
import com.carrotsearch.randomizedtesting.extensions.SystemPropertiesRestoreExtension;

/**
 * Custom test groups.
 */
public class TestTestFiltering extends WithNestedTestClass {
  @RegisterExtension
  SystemPropertiesRestoreExtension restoreProperties = new SystemPropertiesRestoreExtension(); 

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Inherited
  @TestGroup(enabled = false)
  public static @interface Foo {
  }

  static AtomicInteger counter = new AtomicInteger();

  @ExtendWith(RandomizedExtension.class)
  public static class Nested1 extends RandomizedTest {
    @Test @Foo
    public void test1() {
      counter.incrementAndGet();
    }
  }

  @Test
  public void filterVsRulePriority() {
    System.setProperty(getSysProperty(Foo.class), "false");

    // Don't run by default (group is disabled by default).
    counter.set(0);
    System.setProperty(SysGlobals.SYSPROP_TESTFILTER(), "");
    FullResult result1 = runTests(Nested1.class);
    // Note: In JUnit 5, disabled tests are skipped, not assumption-ignored
    Assertions.assertThat(result1.getRunCount() + result1.getIgnoreCount()).isGreaterThanOrEqualTo(1);
    Assertions.assertThat(counter.get()).isEqualTo(0);

    // Run @foo methods even though the group is disabled (but the filtering rule takes priority).
    counter.set(0);
    System.setProperty(SysGlobals.SYSPROP_TESTFILTER(), "@foo");
    FullResult result2 = runTests(Nested1.class);
    Assertions.assertThat(result2.getRunCount()).isGreaterThanOrEqualTo(1);
    Assertions.assertThat(counter.get()).isEqualTo(1);

    // Run the "default" filter.
    counter.set(0);
    System.setProperty(SysGlobals.SYSPROP_TESTFILTER(), "default");
    FullResult result3 = runTests(Nested1.class);
    Assertions.assertThat(counter.get()).isEqualTo(0);
  }
}

package com.carrotsearch.randomizedtesting;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.annotations.Repeat;
import com.carrotsearch.randomizedtesting.extensions.RepeatExtension;

public class TestTargetMethod extends WithNestedTestClass {
  public static class Nested extends RandomizedTest {
    @BeforeEach
    public void checkInHook() {
      assumeRunningNested();
    }

    @TestTemplate
    @ExtendWith(RepeatExtension.class)
    @Repeat(iterations = 3)
    public void testOne() {
      Assertions.assertThat(RandomizedContext.current().getTargetMethod().getName())
        .isEqualTo("testOne");
    }
    
    @Test
    public void testTwo() {
      Assertions.assertThat(RandomizedContext.current().getTargetMethod().getName())
        .isEqualTo("testTwo");
    }
    
    @AfterAll
    @BeforeAll
    public static void staticHooks() {
      Assertions.assertThat(RandomizedContext.current().getTargetMethod())
        .isNull();
    }
  }

  @Test
  public void testTargetMethodAvailable() {
    checkTestsOutput(4, 0, 0, 0, Nested.class);
  }
}

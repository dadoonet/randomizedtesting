package com.carrotsearch.randomizedtesting.contracts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.WithNestedTestClass;

/**
 * Check assumptions at suite level (in {@link BeforeClass}).
 */
public class TestAssumptionsAtClassLevel extends WithNestedTestClass {
  static final List<String> callOrder = new ArrayList<String>();

  /**
   * Test superclass.
   */
  public static class Super extends RandomizedTest {
    @BeforeAll
    public static void beforeClassSuper() {
      assumeRunningNested();
      callOrder.add("beforeClassSuper");
      assumeTrue(false);
    }

    @AfterAll
    public static void afterClassSuper() {
      callOrder.add("afterClassSuper");
    }
  }

  /** 
   * Test subclass.
   */
  public static class SubSub extends Super {
    @BeforeAll
    public static void beforeClass() {
      callOrder.add("beforeClassSub");
    }

    @BeforeEach
    public void beforeTestSub() {
      callOrder.add("beforeTestSub");
    }
    
    @Test
    public void testMethod() {
      callOrder.add("testMethodSub");
    }

    @AfterEach
    public void afterTestSub() {
      callOrder.add("afterTestSub");
    }
    
    @AfterAll
    public static void afterClass() {
      callOrder.add("afterClassSub");
    }
  }

  @BeforeEach
  public void cleanup() {
    callOrder.clear();
  }

  @Test
  public void checkOrder() {
    // In JUnit 5, when @BeforeAll fails with assumption:
    // - run=0: no tests executed
    // - ignored=0: tests are not individually "skipped", container is aborted
    // - fail=0: no failures
    // - assumption=1: container-level assumption failure
    checkTestsOutput(0, 0, 0, 1, SubSub.class);

    List<String> expected = Arrays.asList(
        "beforeClassSuper",
        "afterClassSub",
        "afterClassSuper"
    );
    assertEquals(expected, callOrder);
  }
}

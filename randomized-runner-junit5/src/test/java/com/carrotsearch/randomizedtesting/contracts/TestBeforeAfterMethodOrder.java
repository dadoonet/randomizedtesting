package com.carrotsearch.randomizedtesting.contracts;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.carrotsearch.randomizedtesting.RandomizedContext;
import com.carrotsearch.randomizedtesting.RandomizedExtension;
import com.carrotsearch.randomizedtesting.WithNestedTestClass;
import com.carrotsearch.randomizedtesting.annotations.Repeat;
import com.carrotsearch.randomizedtesting.annotations.Seed;

/**
 * Hooks ordering with respect to class hierarchies.
 */
public class TestBeforeAfterMethodOrder extends WithNestedTestClass {
  static final List<String> callOrder = new ArrayList<String>();

  /**
   * Extension that records before/after calls.
   */
  public static class RecordingExtension implements BeforeEachCallback, AfterEachCallback {
    private final String name;
    
    public RecordingExtension(String name) {
      this.name = name;
    }
    
    public RecordingExtension() {
      this.name = "extension";
    }
    
    @Override
    public void beforeEach(ExtensionContext context) {
      callOrder.add(name + "-before");
    }
    
    @Override
    public void afterEach(ExtensionContext context) {
      callOrder.add(name + "-after");
    }
  }
  
  /**
   * Test superclass.
   */
  @ExtendWith(RandomizedExtension.class)
  public static class Super {
    @BeforeAll
    public static void beforeClassSuper() {
      callOrder.add("beforeClassSuper");
    }

    @BeforeEach
    public final void beforeTest() {
      callOrder.add("beforeTestSuper");
    }

    protected void testMethod() {
      throw new RuntimeException("Should be overridden and public.");
    }

    @AfterEach
    public final void afterTest() {
      callOrder.add("afterTestSuper");
    }
    
    @AfterAll
    public static void afterClassSuper() {
      callOrder.add("afterClassSuper");
    }
  }

  /** 
   * Test subclass.
   */
  @ExtendWith(RandomizedExtension.class)
  public static class SubSub extends Super {
    @BeforeAll
    public static void beforeClass() {
      callOrder.add("  beforeClassSub");
    }

    @BeforeEach
    public void beforeTestSub() {
      callOrder.add("  beforeTestSub");
    }
    
    @Test
    public void testMethod() {
      callOrder.add("    testMethodSub");
    }

    @AfterEach
    public void afterTestSub() {
      callOrder.add("  afterTestSub");
    }
    
    @AfterAll
    public static void afterClass() {
      callOrder.add("  afterClassSub");
    }
  }

  /** 
   * Test subclass with fixed seed.
   */
  @ExtendWith(RandomizedExtension.class)
  @Seed("deadbeef")
  public static class SubSubFixedSeed extends Super {
    @BeforeAll
    public static void beforeClass() {
      callOrder.add("beforeClassSubFS");
    }

    @BeforeEach
    public void beforeTestSub() {
      assumeRunningNested();
      callOrder.add("beforeTestSubFS");
    }

    @Test @Repeat(iterations = 10)
    public void testMethod1() {
      callOrder.add("testMethodSubFS1 " 
          + RandomizedContext.current().getRandom().nextInt());
    }

    @Test @Repeat(iterations = 10)
    public void testMethod2() {
      callOrder.add("testMethodSubFS2 " 
          + RandomizedContext.current().getRandom().nextInt());
    }

    @AfterEach
    public void afterTestSub() {
      callOrder.add("afterTestSubFS");
    }
    
    @AfterAll
    public static void afterClass() {
      callOrder.add("afterClassSubFS");
    }
  }

  @BeforeEach
  public void cleanup() {
    callOrder.clear();
  }

  @Test
  public void checkOrder() throws Exception {
    callOrder.clear();
    FullResult result = runTests(SubSub.class);
    
    assertEquals(1, result.getRunCount());
    assertEquals(0, result.getFailureCount());
    
    // Verify order: beforeClass (super first), beforeEach (super first), test, afterEach, afterClass
    Assertions.assertThat(callOrder).contains("beforeClassSuper");
    Assertions.assertThat(callOrder).contains("  beforeClassSub");
    Assertions.assertThat(callOrder).contains("beforeTestSuper");
    Assertions.assertThat(callOrder).contains("  beforeTestSub");
    Assertions.assertThat(callOrder).contains("    testMethodSub");
    Assertions.assertThat(callOrder).contains("  afterTestSub");
    Assertions.assertThat(callOrder).contains("afterTestSuper");
    Assertions.assertThat(callOrder).contains("  afterClassSub");
    Assertions.assertThat(callOrder).contains("afterClassSuper");
  }

  @Test
  public void checkOrderFixedSeed() throws Exception {
    callOrder.clear();
    runTests(SubSubFixedSeed.class);
    ArrayList<String> order1 = new ArrayList<String>(callOrder);
    
    callOrder.clear();
    runTests(SubSubFixedSeed.class);
    ArrayList<String> order2 = new ArrayList<String>(callOrder);
    
    // With fixed seed, random values should be the same
    assertEquals(order1, order2);
  }
}

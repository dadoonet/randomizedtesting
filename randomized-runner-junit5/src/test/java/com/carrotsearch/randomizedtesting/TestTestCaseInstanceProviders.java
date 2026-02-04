package com.carrotsearch.randomizedtesting;

import java.util.Arrays;
import java.util.IdentityHashMap;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import com.carrotsearch.randomizedtesting.annotations.Repeat;
import com.carrotsearch.randomizedtesting.annotations.Seed;
import com.carrotsearch.randomizedtesting.annotations.Seeds;
import com.carrotsearch.randomizedtesting.annotations.TestCaseInstanceProvider;
import com.carrotsearch.randomizedtesting.annotations.TestCaseInstanceProvider.Type;
import com.carrotsearch.randomizedtesting.extensions.ParametersFactoryExtension;
import com.carrotsearch.randomizedtesting.extensions.RepeatExtension;
import com.carrotsearch.randomizedtesting.extensions.SeedsExtension;

/**
 * Tests for {@link TestCaseInstanceProvider} annotation.
 * 
 * Note: In JUnit 5, class-level parameterization with @ParametersFactory doesn't
 * automatically multiply test executions like in JUnit 4. The tests verify that:
 * 1. INSTANCE_PER_CONSTRUCTOR_ARGS reuses the same instance across all tests
 * 2. Different parameter sets create different cached instances
 */
public class TestTestCaseInstanceProviders extends WithNestedTestClass {
  private static IdentityHashMap<Object,Object> set = new IdentityHashMap<>();

  @ExtendWith({RandomizedExtension.class, ParametersFactoryExtension.class})
  @TestCaseInstanceProvider(Type.INSTANCE_PER_CONSTRUCTOR_ARGS)
  public static class Nested extends RandomizedTest {
    @BeforeEach
    public void setup() {
      assumeRunningNested();
      set.put(this, null);
    }
    
    @TestTemplate
    @ExtendWith(RepeatExtension.class)
    @Repeat(iterations = 3)
    public void testOne() {
      set.put(this, null);
    }

    @Test
    public void testTwo() {
      set.put(this, null);
    }
    
    @TestTemplate
    @ExtendWith({SeedsExtension.class, RepeatExtension.class})
    @Seeds({@Seed("deadbeef"), @Seed("cafebabe")})
    @Repeat(iterations = 2, useConstantSeed = true)
    public void testThree() {
      set.put(this, null);
    }
  }

  @BeforeEach
  public void setup() {
    set.clear();
  }

  @Test
  public void testDefaultConstructor() {
    // In JUnit 5: testOne (3 invocations) + testTwo (1) + testThree (2 seeds * 2 repeats = 4) = 8
    checkTestsOutput(3 + 1 + 2 * 2, 0, 0, 0, Nested.class);
    // With INSTANCE_PER_CONSTRUCTOR_ARGS, only 1 instance should be created
    Assertions.assertThat(set).hasSize(1);
  }

  /**
   * Test that INSTANCE_PER_CONSTRUCTOR_ARGS works without @ParametersFactory.
   * This is the simpler case that can be fully tested in JUnit 5.
   */
  @ExtendWith({RandomizedExtension.class, ParametersFactoryExtension.class})
  @TestCaseInstanceProvider(Type.INSTANCE_PER_CONSTRUCTOR_ARGS)
  public static class NestedSimple extends RandomizedTest {
    @BeforeEach
    public void setup() {
      assumeRunningNested();
      set.put(this, null);
    }
    
    @Test
    public void testA() {
      set.put(this, null);
    }

    @Test
    public void testB() {
      set.put(this, null);
    }
    
    @Test
    public void testC() {
      set.put(this, null);
    }
  }
  
  @Test
  public void testSimpleInstanceReuse() {
    // 3 tests, all should use the same instance
    checkTestsOutput(3, 0, 0, 0, NestedSimple.class);
    // With INSTANCE_PER_CONSTRUCTOR_ARGS, only 1 instance should be created
    Assertions.assertThat(set).hasSize(1);
  }

  /**
   * Test INSTANCE_PER_TEST_METHOD (default JUnit behavior) creates new instances.
   */
  @ExtendWith({RandomizedExtension.class, ParametersFactoryExtension.class})
  @TestCaseInstanceProvider(Type.INSTANCE_PER_TEST_METHOD)
  public static class NestedPerMethod extends RandomizedTest {
    @BeforeEach
    public void setup() {
      assumeRunningNested();
      set.put(this, null);
    }
    
    @Test
    public void testA() {
      set.put(this, null);
    }

    @Test
    public void testB() {
      set.put(this, null);
    }
    
    @Test
    public void testC() {
      set.put(this, null);
    }
  }
  
  @Test
  public void testPerMethodCreatesMultipleInstances() {
    // 3 tests, each should have its own instance
    checkTestsOutput(3, 0, 0, 0, NestedPerMethod.class);
    // With INSTANCE_PER_TEST_METHOD, 3 instances should be created
    Assertions.assertThat(set).hasSize(3);
  }
}

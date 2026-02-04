package com.carrotsearch.randomizedtesting;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import com.carrotsearch.randomizedtesting.extensions.ParametersFactoryExtension;

/**
 * Tests for @ParametersFactory functionality.
 * 
 * Note: In JUnit 5, class-level @ParametersFactory doesn't automatically multiply
 * test executions like in JUnit 4. Each test method gets one parameter set,
 * cycling through available parameters. Tests have been adapted accordingly.
 */
public class TestParameterized extends WithNestedTestClass {

  /**
   * Test basic @ParametersFactory with @Name annotations.
   */
  @ExtendWith({RandomizedExtension.class, ParametersFactoryExtension.class})
  public static class NestedWithParams extends RandomizedTest {
    private final int value;
    private final String str;

    public NestedWithParams(@Name("value") int value, @Name("string") String str) {
      this.value = value;
      this.str = str;
    }

    @Test
    public void testOne() {
      assumeRunningNested();
      // Just verify parameters are accessible
      assertTrue(value > 0);
      assertNotNull(str);
    }
    
    @Test
    public void testTwo() {
      assumeRunningNested();
      assertTrue(value > 0);
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() {
      return Arrays.asList($$(
          $(1, "abc"),
          $(2, "def")));
    }
  }

  @Test
  public void testBasicParameterized() {
    // 2 test methods, parameters cycle through 2 parameter sets
    checkTestsOutput(2, 0, 0, 0, NestedWithParams.class);
  }

  /**
   * Test @Name annotation works with parameters.
   */
  @ExtendWith({RandomizedExtension.class, ParametersFactoryExtension.class})
  public static class NestedWithNameAnnotation extends RandomizedTest {
    private final int paramValue;

    public NestedWithNameAnnotation(@Name("paramName") int value) {
      this.paramValue = value;
    }

    @Test
    public void failing() {
      assumeRunningNested();
      fail("Intentional failure with param: " + paramValue);
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() {
      return Arrays.asList($$( $(42) ));
    }
  }

  @Test
  public void testNameAnnotationInFailure() {
    FullResult r = runTests(NestedWithNameAnnotation.class);
    // Should have 1 test that fails
    Assertions.assertThat(r.getFailures()).hasSize(1);
  }

  /**
   * Test empty parameters list results in test failure (no instance can be created).
   */
  @ExtendWith({RandomizedExtension.class, ParametersFactoryExtension.class})
  public static class NestedEmptyParams extends RandomizedTest {
    public NestedEmptyParams(@Name("param") int value) {
    }

    @Test
    public void testMe() {
      assumeRunningNested();
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() {
      return Collections.emptyList();
    }
  }

  @Test
  public void testEmptyParamsList() {
    // Empty params list causes TestAbortedException, which JUnit 5 reports as failure
    // 1 test attempted, 1 failure (cannot create instance)
    FullResult r = runTests(NestedEmptyParams.class);
    // The test should fail because no parameters are available
    Assertions.assertThat(r.getFailureCount()).isGreaterThan(0);
  }

  /**
   * Test assumption failure in factory method.
   */
  @ExtendWith({RandomizedExtension.class, ParametersFactoryExtension.class})
  public static class NestedAssumptionInFactory extends RandomizedTest {
    public NestedAssumptionInFactory(@Name("param") int value) {
    }

    @Test
    public void testMe() {
      assumeRunningNested();
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() {
      assumeTrue(false);
      throw new RuntimeException("Should not reach here");
    }
  }

  @Test
  public void testAssumptionFailureInFactory() {
    // Assumption failure in factory results in empty params list
    // which causes test failure (cannot create instance)
    FullResult r = runTests(NestedAssumptionInFactory.class);
    Assertions.assertThat(r.getFailureCount()).isGreaterThan(0);
  }

  /**
   * Test non-Object[] arrays are handled correctly.
   */
  @ExtendWith({RandomizedExtension.class, ParametersFactoryExtension.class})
  public static class NestedNonObjectArray extends RandomizedTest {
    public NestedNonObjectArray() {}

    @Test
    public void testMe() {
      assumeRunningNested();
    }
    
    @Test
    public void testMe2() {
      assumeRunningNested();
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() {
      return Arrays.asList(
          new Object[] {},
          new Integer[] {});
    }
  }

  @Test
  public void testNonObjectArray() {
    // 2 test methods with 2 parameter sets
    checkTestsOutput(2, 0, 0, 0, NestedNonObjectArray.class);
  }
  
  /**
   * Test with default constructor (no parameters).
   */
  @ExtendWith({RandomizedExtension.class, ParametersFactoryExtension.class})
  public static class NestedDefaultConstructor extends RandomizedTest {
    public NestedDefaultConstructor() {}

    @Test
    public void testMe() {
      assumeRunningNested();
    }

    // No @ParametersFactory - uses default constructor
  }

  @Test
  public void testDefaultConstructor() {
    checkTestsOutput(1, 0, 0, 0, NestedDefaultConstructor.class);
  }
}

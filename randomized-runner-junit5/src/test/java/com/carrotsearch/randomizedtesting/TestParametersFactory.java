package com.carrotsearch.randomizedtesting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import com.carrotsearch.randomizedtesting.extensions.ParametersFactoryExtension;

/**
 * Test for {@link ParametersFactory} support.
 * 
 * <p><b>Note:</b> JUnit 5's test model differs from JUnit 4's parameterized tests.
 * In JUnit 4, each parameter set creates a new test class instance.
 * In JUnit 5, the TestInstanceFactory approach only creates one instance per test method.
 * 
 * <p>For full parameterized test support in JUnit 5, consider using:
 * <ul>
 *   <li>{@code @ParameterizedTest} with {@code @MethodSource}</li>
 *   <li>{@code @TestTemplate} with custom extension</li>
 * </ul>
 * 
 * <p>This extension provides basic support for constructor injection from parameters.
 */
public class TestParametersFactory extends WithNestedTestClass {
  
  /**
   * Test class with constructor parameters.
   * Note: Only the first parameter set is used (JUnit 5 limitation).
   */
  @ExtendWith({RandomizedExtension.class, ParametersFactoryExtension.class})
  public static class NestedWithParams extends RandomizedTest {
    private final int value;
    private final String name;
    
    public NestedWithParams(@Name("value") int value, @Name("name") String name) {
      this.value = value;
      this.name = name;
    }
    
    @Test
    public void testValueIsPositive() {
      assumeRunningNested();
      assertThat(value).isGreaterThan(0);
    }
    
    @Test
    public void testNameNotEmpty() {
      assumeRunningNested();
      assertThat(name).isNotEmpty();
    }
    
    @ParametersFactory(shuffle = false)
    public static Iterable<Object[]> parameters() {
      return Arrays.asList(
          new Object[]{1, "first"},
          new Object[]{2, "second"}
      );
    }
  }
  
  @Test
  public void testConstructorInjection() {
    FullResult result = runTests(NestedWithParams.class);
    
    // Tests run with first parameter set (JUnit 5 TestInstanceFactory limitation)
    // Each test method runs once, instance is created for each method
    assertThat(result.getRunCount()).isEqualTo(2);
    assertThat(result.getFailureCount()).isEqualTo(0);
  }
  
  /**
   * Test without @ParametersFactory - uses default constructor.
   */
  @ExtendWith({RandomizedExtension.class, ParametersFactoryExtension.class})
  public static class NestedNoParams extends RandomizedTest {
    @Test
    public void testSimple() {
      assumeRunningNested();
      // Just verifies that tests run without @ParametersFactory
    }
  }
  
  @Test
  public void testNoParametersFactory() {
    FullResult result = runTests(NestedNoParams.class);
    assertThat(result.getRunCount()).isEqualTo(1);
    assertThat(result.getFailureCount()).isEqualTo(0);
  }
}

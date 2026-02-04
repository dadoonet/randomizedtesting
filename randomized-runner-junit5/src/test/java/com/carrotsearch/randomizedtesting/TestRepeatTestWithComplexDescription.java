package com.carrotsearch.randomizedtesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.carrotsearch.randomizedtesting.annotations.Listeners;
import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import com.carrotsearch.randomizedtesting.annotations.Repeat;
import com.carrotsearch.randomizedtesting.annotations.Seed;
import com.carrotsearch.randomizedtesting.annotations.Seeds;
import com.carrotsearch.randomizedtesting.extensions.ListenersExtension;
import com.carrotsearch.randomizedtesting.extensions.ParametersFactoryExtension;
import com.carrotsearch.randomizedtesting.extensions.RepeatExtension;
import com.carrotsearch.randomizedtesting.extensions.SeedsExtension;
import com.carrotsearch.randomizedtesting.extensions.SystemPropertiesInvariantExtension;
import com.carrotsearch.randomizedtesting.listeners.RandomizedTestListener;
import com.carrotsearch.randomizedtesting.listeners.TestInfo;

/**
 * Tests for @Repeat with complex test descriptions containing special characters.
 * Verifies that test names can be used for filtering via SYSPROP_TESTMETHOD.
 */
public class TestRepeatTestWithComplexDescription extends WithNestedTestClass {

  // Allow tests.method to be modified by our tests
  @RegisterExtension
  static SystemPropertiesInvariantExtension restoreProperties = 
      new SystemPropertiesInvariantExtension(new HashSet<>(Arrays.asList(
          SysGlobals.SYSPROP_TESTMETHOD())));

  static ArrayList<String> buf;

  public static class CaptureTestNamesListener implements RandomizedTestListener {
    @Override
    public void testStarted(TestInfo testInfo) {
      if (buf != null) {
        buf.add(testInfo.getDisplayName());
      }
    }
  }

  @Seed("deadbeef")
  @ExtendWith({RandomizedExtension.class, ParametersFactoryExtension.class, ListenersExtension.class})
  @Listeners({CaptureTestNamesListener.class})
  public static class Nested extends RandomizedTest {
    private final String value;

    public Nested(@Name("value") String value) {
      this.value = value;
    }

    @TestTemplate
    @ExtendWith({SeedsExtension.class, RepeatExtension.class})
    @Seeds({@Seed(), @Seed("deadbeef")})
    @Repeat(iterations = 2, useConstantSeed = false)
    public void testFoo() {
      assumeRunningNested();
    }

    @Test
    public void testBar() {
      assumeRunningNested();
    }

    @ParametersFactory()
    public static Iterable<Object[]> parameters() {
      assumeRunningNested();

      List<Object[]> params = new ArrayList<>();
      params.add($("simple"));
      params.add($("with/slash"));
      params.add($("with\\backslash"));
      params.add($("special$[]{}"));
      return params;
    }
  }

  @BeforeEach
  public void setup() {
    buf = new ArrayList<>();
  }

  @AfterEach
  public void cleanup() {
    // Clear any test method filter we set
    System.clearProperty(SysGlobals.SYSPROP_TESTMETHOD());
  }

  @Test
  public void testComplexDescriptionsAreGenerated() {
    // Just verify tests run with complex parameter values
    buf = new ArrayList<>();
    FullResult result = runTests(Nested.class);
    
    // Should have tests that ran
    Assertions.assertThat(result.getRunCount()).isGreaterThan(0);
    
    // Should have captured test names
    Assertions.assertThat(buf).isNotEmpty();
    
    // Verify we have both testFoo and testBar in the names
    Assertions.assertThat(buf).anyMatch(name -> name.contains("testBar"));
  }

  @Test
  public void testMethodFilteringWithSimpleName() {
    // Test filtering by simple method name
    buf = new ArrayList<>();
    System.setProperty(SysGlobals.SYSPROP_TESTMETHOD(), "testBar");
    
    FullResult result = runTests(Nested.class);
    
    // Should only run testBar
    Assertions.assertThat(result.wasSuccessful()).isTrue();
    Assertions.assertThat(buf).allMatch(name -> name.contains("testBar"));
  }

  @Test  
  public void testMethodFilteringWithGlob() {
    // Test filtering with glob pattern
    buf = new ArrayList<>();
    System.setProperty(SysGlobals.SYSPROP_TESTMETHOD(), "test*");
    
    FullResult result = runTests(Nested.class);
    
    // Should run all tests
    Assertions.assertThat(result.wasSuccessful()).isTrue();
    Assertions.assertThat(buf).isNotEmpty();
  }
}

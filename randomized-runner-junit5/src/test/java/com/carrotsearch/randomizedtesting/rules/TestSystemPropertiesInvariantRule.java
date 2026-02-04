package com.carrotsearch.randomizedtesting.rules;

import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.carrotsearch.randomizedtesting.RandomizedExtension;
import com.carrotsearch.randomizedtesting.WithNestedTestClass;
import com.carrotsearch.randomizedtesting.extensions.SystemPropertiesInvariantExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for SystemPropertiesInvariantExtension.
 */
public class TestSystemPropertiesInvariantRule extends WithNestedTestClass {
  public static final String PROP_KEY1 = "new-property-1";
  public static final String VALUE1 = "new-value-1";
  
  @ExtendWith({RandomizedExtension.class})
  public static class Base {
    @RegisterExtension
    static SystemPropertiesInvariantExtension sysPropsInvariant = 
        new SystemPropertiesInvariantExtension();

    @Test
    public void testEmpty() {
      assumeRunningNested();
    }
  }
  
  @ExtendWith({RandomizedExtension.class})
  public static class InBeforeClass {
    @RegisterExtension
    static SystemPropertiesInvariantExtension sysPropsInvariant = 
        new SystemPropertiesInvariantExtension();

    @BeforeAll
    public static void beforeClass() {
      if (!isRunningNested()) return;
      System.setProperty(PROP_KEY1, VALUE1);
    }
    
    @Test
    public void testEmpty() {
      assumeRunningNested();
    }
  }
  
  @ExtendWith({RandomizedExtension.class})
  public static class InAfterClass {
    @RegisterExtension
    static SystemPropertiesInvariantExtension sysPropsInvariant = 
        new SystemPropertiesInvariantExtension();

    @Test
    public void testEmpty() {
      assumeRunningNested();
    }

    @AfterAll
    public static void afterClass() {
      if (!isRunningNested()) return;
      System.setProperty(PROP_KEY1, VALUE1);
    }
  }
  
  @ExtendWith({RandomizedExtension.class})
  public static class InTestMethod {
    @RegisterExtension
    SystemPropertiesInvariantExtension sysPropsInvariant = 
        new SystemPropertiesInvariantExtension();

    @Test
    public void testMethod1() {
      assumeRunningNested();
      if (System.getProperty(PROP_KEY1) != null) {
        throw new RuntimeException("Shouldn't be here.");
      }
      System.setProperty(PROP_KEY1, VALUE1);
    }
    
    @Test
    public void testMethod2() {
      assumeRunningNested();
      if (System.getProperty(PROP_KEY1) != null) {
        throw new RuntimeException("Shouldn't be here.");
      }
      System.setProperty(PROP_KEY1, VALUE1);
    }
  }

  @ExtendWith({RandomizedExtension.class})
  public static class NonStringProperties {
    @RegisterExtension
    SystemPropertiesInvariantExtension sysPropsInvariant = 
        new SystemPropertiesInvariantExtension();

    @Test
    public void testMethod1() {
      assumeRunningNested();
      if (System.getProperties().get(PROP_KEY1) != null) {
        throw new RuntimeException("Will pass.");
      }

      Properties properties = System.getProperties();
      properties.put(PROP_KEY1, new Object());
      assertTrue(System.getProperties().get(PROP_KEY1) != null);
    }

    @Test
    public void testMethod2() {
      assumeRunningNested();
      if (System.getProperties().get(PROP_KEY1) != null) {
        throw new RuntimeException("Will pass.");
      }

      Properties properties = System.getProperties();
      properties.put(PROP_KEY1, new Object());
      assertTrue(System.getProperties().get(PROP_KEY1) != null);
    }

    @AfterAll
    public static void cleanup() {
      System.getProperties().remove(PROP_KEY1);
    }
  }

  @Test
  public void testRuleInvariantBeforeClass() {
    FullResult runClasses = runTests(InBeforeClass.class);
    assertEquals(1, runClasses.getFailureCount());
    assertTrue(runClasses.getFailures().get(0).getTrace()
        .contains(PROP_KEY1));
    assertNull(System.getProperty(PROP_KEY1));
  }
  
  @Test
  public void testRuleInvariantAfterClass() {
    FullResult runClasses = runTests(InAfterClass.class);
    assertEquals(1, runClasses.getFailureCount());
    assertTrue(runClasses.getFailures().get(0).getTrace()
        .contains(PROP_KEY1));
    assertNull(System.getProperty(PROP_KEY1));
  }
  
  @Test
  public void testRuleInvariantInTestMethod() {
    FullResult runClasses = runTests(InTestMethod.class);
    assertEquals(2, runClasses.getFailureCount());
    for (FailureInfo f : runClasses.getFailures()) {
      assertTrue(f.getTrace().contains(PROP_KEY1));
    }
    assertNull(System.getProperty(PROP_KEY1));
  }
  
  @Test
  public void testNonStringProperties() {
    FullResult runClasses = runTests(NonStringProperties.class);
    // The extension should detect non-string properties and fail
    assertTrue(runClasses.getFailureCount() >= 1);
  }
}

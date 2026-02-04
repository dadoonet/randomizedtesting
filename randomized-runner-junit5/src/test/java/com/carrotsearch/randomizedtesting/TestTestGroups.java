package com.carrotsearch.randomizedtesting;

import static com.carrotsearch.randomizedtesting.annotations.TestGroup.Utilities.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.annotations.Nightly;
import com.carrotsearch.randomizedtesting.annotations.TestGroup;

/**
 * Custom test groups.
 */
public class TestTestGroups extends WithNestedTestClass {
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Inherited
  @TestGroup(enabled = true)
  public static @interface Group1 {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Inherited
  @TestGroup(enabled = false, name = "abc", sysProperty = "custom.abc")
  public static @interface Group2 {
  }

  @ExtendWith(RandomizedExtension.class)
  public static class Nested1 extends RandomizedTest {
    @Test @Group1 @Group2
    public void test1() {
    }
    
    @BeforeAll
    public static void beforeClass() {
      beforeClassRan = true;
    }
    
    @AfterAll
    public static void afterClass() {
      afterClassRan = true;
    }
  }

  @ExtendWith(RandomizedExtension.class)
  public static class Nested3 extends Nested1 {
    @Test
    public void testUnconditional() {
    }
  }

  @ExtendWith(RandomizedExtension.class)
  @Group1 @Group2
  public static class Nested2 extends RandomizedTest {
    @Test
    public void test1() {
    }
    
    @BeforeAll
    public static void beforeClass() {
      beforeClassRan = true;
    }
    
    @AfterAll
    public static void afterClass() {
      afterClassRan = true;
    }    
  }
  
  public static boolean beforeClassRan;
  public static boolean afterClassRan;
  
  @Test
  public void checkDefaultNames() {
    assertEquals("group1", getGroupName(Group1.class));
    assertEquals("abc", getGroupName(Group2.class));
    assertEquals(SysGlobals.CURRENT_PREFIX() + ".group1", getSysProperty(Group1.class));
    assertEquals("custom.abc", getSysProperty(Group2.class));
    assertEquals(SysGlobals.CURRENT_PREFIX() + ".nightly", getSysProperty(Nightly.class));
    assertEquals("nightly", getGroupName(Nightly.class));
  }  

  @Test
  public void groupsOnMethods() {
    String group1Property = getSysProperty(Group1.class);
    String group2Property = getSysProperty(Group2.class);
    try {
      // In JUnit 5, disabled groups cause tests to be skipped (ignored)
      // Note: @BeforeAll/@AfterAll still run even if all tests are skipped
      afterClassRan = beforeClassRan = false;
      checkTestsOutput(0, 1, 0, 0, Nested1.class);  // Group2 disabled -> test skipped
      // JUnit 5: lifecycle hooks run even when tests are skipped
      assertTrue(afterClassRan);
      assertTrue(beforeClassRan);
      
      afterClassRan = beforeClassRan = false;
      System.setProperty(group1Property, "true");
      checkTestsOutput(0, 1, 0, 0, Nested1.class);  // Group2 still disabled -> test skipped
      assertTrue(afterClassRan);
      assertTrue(beforeClassRan);

      afterClassRan = beforeClassRan = false;
      System.setProperty(group2Property, "true");
      checkTestsOutput(1, 0, 0, 0, Nested1.class);  // Both groups enabled -> test runs
      assertTrue(afterClassRan);
      assertTrue(beforeClassRan);

      afterClassRan = beforeClassRan = false;
      System.setProperty(group1Property, "false");
      checkTestsOutput(0, 1, 0, 0, Nested1.class);  // Group1 disabled -> test skipped
      assertTrue(afterClassRan);
      assertTrue(beforeClassRan);
    } finally {
      System.clearProperty(group1Property);
      System.clearProperty(group2Property);
    }
  }

  @Test
  public void groupsOnASubsetOfMethods() {
    String group1Property = getSysProperty(Group1.class);
    String group2Property = getSysProperty(Group2.class);
    try {
      // Nested3 has 2 tests: test1 (groups) and testUnconditional (no groups)
      // In JUnit 5, disabled groups cause tests to be skipped
      afterClassRan = beforeClassRan = false;
      checkTestsOutput(1, 1, 0, 0, Nested3.class);  // test1 skipped (Group2 disabled), testUnconditional runs
      assertTrue(afterClassRan);
      assertTrue(beforeClassRan);
      
      afterClassRan = beforeClassRan = false;
      System.setProperty(group1Property, "true");
      checkTestsOutput(1, 1, 0, 0, Nested3.class);  // test1 still skipped (Group2 disabled)
      assertTrue(afterClassRan);
      assertTrue(beforeClassRan);

      afterClassRan = beforeClassRan = false;
      System.setProperty(group2Property, "true");
      checkTestsOutput(2, 0, 0, 0, Nested3.class);  // Both groups enabled -> both tests run
      assertTrue(afterClassRan);
      assertTrue(beforeClassRan);

      afterClassRan = beforeClassRan = false;
      System.setProperty(group1Property, "false");
      checkTestsOutput(1, 1, 0, 0, Nested3.class);  // test1 skipped (Group1 disabled)
      assertTrue(afterClassRan);
      assertTrue(beforeClassRan);
    } finally {
      System.clearProperty(group1Property);
      System.clearProperty(group2Property);
    }
  }

  @Test
  public void groupsOnClass() {
    String group1Property = getSysProperty(Group1.class);
    String group2Property = getSysProperty(Group2.class);
    try {
      // Nested2 has @Group1 @Group2 on the class
      // In JUnit 5, class-level group disabling skips the container (class)
      afterClassRan = beforeClassRan = false;
      checkTestsOutput(0, 1, 0, 0, Nested2.class);  // Class skipped (Group2 disabled)
      assertFalse(afterClassRan);
      assertFalse(beforeClassRan);

      afterClassRan = beforeClassRan = false;
      System.setProperty(group1Property, "true");
      checkTestsOutput(0, 1, 0, 0, Nested2.class);  // Class still skipped (Group2 disabled)
      assertFalse(afterClassRan);
      assertFalse(beforeClassRan);

      afterClassRan = beforeClassRan = false;
      System.setProperty(group2Property, "true");
      checkTestsOutput(1, 0, 0, 0, Nested2.class);  // Both groups enabled -> test runs
      assertTrue(afterClassRan);
      assertTrue(beforeClassRan);

      afterClassRan = beforeClassRan = false;
      System.setProperty(group1Property, "false");      
      checkTestsOutput(0, 1, 0, 0, Nested2.class);  // Class skipped (Group1 disabled)
      assertFalse(afterClassRan);
      assertFalse(beforeClassRan);
    } finally {
      System.clearProperty(group1Property);
      System.clearProperty(group2Property);
    }
  }
}

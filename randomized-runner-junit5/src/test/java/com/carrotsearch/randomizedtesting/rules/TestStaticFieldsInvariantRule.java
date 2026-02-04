package com.carrotsearch.randomizedtesting.rules;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.carrotsearch.randomizedtesting.RandomizedExtension;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.WithNestedTestClass;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope.Scope;
import com.carrotsearch.randomizedtesting.extensions.StaticFieldsInvariantExtension;

/**
 * Test for StaticFieldsInvariantExtension.
 */
public class TestStaticFieldsInvariantRule extends WithNestedTestClass {
  static int LEAK_THRESHOLD = 5 * 1024 * 1024;

  @ExtendWith(RandomizedExtension.class)
  @ThreadLeakScope(Scope.SUITE)
  public static class Base extends RandomizedTest {
    @RegisterExtension
    static StaticFieldsInvariantExtension staticFieldsInvariant = 
        new StaticFieldsInvariantExtension(LEAK_THRESHOLD, true);

    @Test
    public void testEmpty() {
      assumeRunningNested();
    }
  }

  @ExtendWith(RandomizedExtension.class)
  @ThreadLeakScope(Scope.SUITE)
  public static class Smaller extends RandomizedTest {
    static byte [] field0; 

    @RegisterExtension
    static StaticFieldsInvariantExtension staticFieldsInvariant = 
        new StaticFieldsInvariantExtension(LEAK_THRESHOLD, true);
    
    @BeforeAll
    private static void setup() {
      field0 = new byte [LEAK_THRESHOLD / 2];
    }

    @Test
    public void testEmpty() {
      assumeRunningNested();
    }
  }

  @ExtendWith(RandomizedExtension.class)
  @ThreadLeakScope(Scope.SUITE)
  public static class Exceeding extends RandomizedTest {
    static byte [] field0;
    static byte [] field1;
    static byte [] field2;
    static int [] field3;
    static long field4;
    final static long [] field5 = new long [1024];

    @RegisterExtension
    static StaticFieldsInvariantExtension staticFieldsInvariant = 
        new StaticFieldsInvariantExtension(LEAK_THRESHOLD, true);

    @BeforeAll
    private static void setup() {
      field0 = new byte [LEAK_THRESHOLD / 2];
      field1 = new byte [LEAK_THRESHOLD / 2];
      field2 = new byte [100];
      field3 = new int [100];
    }

    @Test
    public void testEmpty() {
      assumeRunningNested();
    }
  }

  @ExtendWith(RandomizedExtension.class)
  @ThreadLeakScope(Scope.SUITE)
  public static class MultipleReferences extends RandomizedTest {
    static Object ref1, ref2, ref3,
                  ref4, ref5, ref6;

    static Object ref7 = null;
    
    static {
      Map<String,Object> map = new HashMap<String,Object>();
      map.put("key", new byte [1024 * 1024 * 2]);
      ref1 = ref2 = ref3 = ref4 = ref5 = ref6 = map;
    }

    @RegisterExtension
    static StaticFieldsInvariantExtension staticFieldsInvariant = 
        new StaticFieldsInvariantExtension(LEAK_THRESHOLD, true);

    @Test
    public void testEmpty() {
      assumeRunningNested();
    }
  }

  @Test
  public void testReferencesCountedMultipleTimes() { 
    FullResult runClasses = runTests(MultipleReferences.class);
    Assertions.assertThat(runClasses.getFailures()).isEmpty();
  }

  @Test
  public void testPassingUnderThreshold() {
    FullResult runClasses = runTests(Smaller.class);
    Assertions.assertThat(runClasses.getFailures()).isEmpty();
  }
  
  @Test
  public void testFailingAboveThreshold() {
    FullResult runClasses = runTests(Exceeding.class);
    Assertions.assertThat(runClasses.getFailures()).hasSize(1);
    
    Assertions.assertThat(runClasses.getFailures().get(0).getTrace())
      .contains(".field0")
      .contains(".field1")
      .contains(".field2")
      .contains(".field3")
      .doesNotContain(".field5");
  }
  
  static class Holder {
    private final Path path;
    
    Holder() {
      this.path = Paths.get(".");
      final String name = this.path.getClass().getName();
      RandomizedTest.assumeTrue(Path.class.getName() + " is not implemented by internal class in this JVM: " + name,
        name.startsWith("sun.") || name.startsWith("jdk."));
    }
  }
  
  @ExtendWith(RandomizedExtension.class)
  @ThreadLeakScope(Scope.SUITE)
  public static class FailsJava9 extends RandomizedTest {
    static Holder field0;

    @RegisterExtension
    static StaticFieldsInvariantExtension staticFieldsInvariant = 
        new StaticFieldsInvariantExtension(LEAK_THRESHOLD, true);
    
    @BeforeAll
    private static void setup() throws Exception {
      field0 = new Holder();
    }

    @Test
    public void testEmpty() {
      assumeRunningNested();
    }
  }

  @Test @Disabled
  public void testJava9Jigsaw() {
    // check if we have Java 9 module system:
    try {
      Class.class.getMethod("getModule");
    } catch (Exception e) {
      RandomizedTest.assumeTrue("This test requires Java 9 module system (Jigsaw)", false);
    }
  
    FullResult runClasses = runTests(FailsJava9.class);
    Assertions.assertThat(runClasses.getFailures()).hasSize(1);
    
    Assertions.assertThat(runClasses.getFailures().get(0).getTrace())
      .contains("sizes cannot be measured due to security restrictions or Java 9")
      .contains(".field0");
  }
}

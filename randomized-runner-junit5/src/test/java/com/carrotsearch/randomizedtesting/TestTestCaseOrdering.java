package com.carrotsearch.randomizedtesting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.annotations.TestCaseOrdering;
import com.carrotsearch.randomizedtesting.extensions.TestCaseOrderingExtension;

/**
 * Tests for {@link TestCaseOrdering} annotation support in JUnit 5.
 */
public class TestTestCaseOrdering extends WithNestedTestClass {
  static List<String> buf;

  @ExtendWith(RandomizedExtension.class)
  @TestMethodOrder(TestCaseOrderingExtension.class)
  @TestCaseOrdering(TestCaseOrdering.AlphabeticOrder.class)
  public static class Alphabetical extends RandomizedTest {
    @BeforeEach 
    public void assumeNested() { assumeRunningNested(); }
    
    @Test public void a() { buf.add("a"); }
    @Test public void b() { buf.add("b"); }
    @Test public void c() { buf.add("c"); }
    @Test public void d() { buf.add("d"); }
  }

  @BeforeEach
  public void clean() {
    buf = new ArrayList<>();
  }

  @Test
  public void testAlphabetical() {
    runTests(Alphabetical.class);
    assertThat(buf).containsExactly("a", "b", "c", "d");
  }
  
  // Note: testAlphabeticalWithRepetitions and testAlphabeticalWithParameters
  // are not supported in this JUnit 5 implementation because they require
  // @Repeat and @ParametersFactory which have different semantics in JUnit 5.
  // The ordering of repetitions/parameters would need a TestTemplateInvocationContextProvider
  // that handles ordering across all invocations.
}

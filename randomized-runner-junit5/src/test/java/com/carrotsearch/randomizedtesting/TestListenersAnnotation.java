package com.carrotsearch.randomizedtesting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.annotations.Listeners;
import com.carrotsearch.randomizedtesting.extensions.ListenersExtension;
import com.carrotsearch.randomizedtesting.listeners.RandomizedTestListener;
import com.carrotsearch.randomizedtesting.listeners.TestInfo;

/**
 * Test listeners on suite.
 */
public class TestListenersAnnotation extends WithNestedTestClass {
  
  public static List<String> buffer = new ArrayList<>();

  public static class NoopListener implements RandomizedTestListener {
  }

  public static class BufferAppendListener implements RandomizedTestListener {
    @Override
    public void testRunStarted() {
      buffer.add("run started");
    }
    
    @Override
    public void testStarted(TestInfo testInfo) {
      buffer.add("test started: " + getMethodName(testInfo));
    }
    
    @Override
    public void testFinished(TestInfo testInfo) {
      buffer.add("test finished: " + getMethodName(testInfo));
    }
    
    @Override
    public void testAborted(TestInfo testInfo, Throwable cause) {
      buffer.add("assumption failed: " + getMethodName(testInfo));
    }

    @Override
    public void testSkipped(TestInfo testInfo, String reason) {
      buffer.add("test ignored: " + getMethodName(testInfo));
    }
    
    @Override
    public void testFailed(TestInfo testInfo, Throwable cause) {
      buffer.add("failure: " + getMethodName(testInfo));
    }

    @Override
    public void testRunFinished(int runCount) {
      buffer.add("run finished: " + runCount);
    }
    
    private String getMethodName(TestInfo testInfo) {
      return testInfo.getTestMethod().map(m -> m.getName()).orElse(null);
    }
  }

  @ExtendWith({RandomizedExtension.class, ListenersExtension.class})
  @Listeners({BufferAppendListener.class})
  public static class Nested1 extends RandomizedTest {
  }

  @Listeners({NoopListener.class})
  public static class Nested2 extends Nested1 {
    @BeforeEach
    public void assumeNested() { assumeRunningNested(); }
    
    @Test @Disabled
    public void ignored() {
    }

    @Test
    public void passing() throws Exception {
    }
    
    @Test
    public void failing() throws Exception {
      org.junit.jupiter.api.Assertions.fail();
    }

    @Test
    public void assumptionFailing() throws Exception {
      assumeTrue(false);
    }
  }

  @BeforeEach
  public void clean() {
    buffer.clear();
  }

  @Test
  public void checkListeners() {
    runTests(Nested2.class);
    // Perhaps this is overly simple, but we just want to know that it executed.
    assertThat(buffer).isNotEmpty();
    
    // Verify we got the expected events
    assertThat(buffer).contains("run started");
    assertThat(buffer).anyMatch(s -> s.startsWith("test started:"));
    assertThat(buffer).anyMatch(s -> s.startsWith("run finished:"));
  }
}

package com.carrotsearch.ant.tasks.junit5.tests;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.RandomizedExtension;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.Listeners;
import com.carrotsearch.randomizedtesting.listeners.RandomizedTestListener;
import com.carrotsearch.randomizedtesting.listeners.TestInfo;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(RandomizedExtension.class)
@Listeners({
  SuiteListeners.PrintEventListener.class
})
public class SuiteListeners extends RandomizedTest {
  
  public static class PrintEventListener implements RandomizedTestListener {

    @Override
    public void testRunStarted() {
      System.out.println("testRunStarted");
    }

    @Override
    public void testRunFinished(int runCount) {
      System.out.println("testRunFinished.");
    }

    @Override
    public void testStarted(TestInfo testInfo) {
      System.out.println("testStarted: " + testInfo.getDisplayName());
    }

    @Override
    public void testFinished(TestInfo testInfo) {
      System.out.println("testFinished: " + testInfo.getDisplayName());
    }

    @Override
    public void testFailed(TestInfo testInfo, Throwable failure) {
      System.out.println("testFailure: " + failure);
    }

    @Override
    public void testAborted(TestInfo testInfo, Throwable reason) {
      System.out.println("testAssumptionFailure: " + reason);
    }

    @Override
    public void testSkipped(TestInfo testInfo, String reason) {
      System.out.println("testIgnored: " + reason);
    }
  }

  @Test 
  public void passing() {}
  
  @Test @Disabled  
  public void ignored() {}
  
  @Test 
  public void aignored() { Assumptions.assumeTrue(false); }

  @Test 
  public void failure() { fail(); }
}

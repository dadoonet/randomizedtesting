package com.carrotsearch.ant.tasks.junit5.it;

import org.junit.jupiter.api.Test;

public class TestIfNoTestsOption extends JUnit5XmlTestBase {
  @Test
  public void allFilteredOut() {
    expectBuildExceptionContaining("allFilteredOut", "There were no executed tests: 0 suites, 0 tests");
  }

  @Test
  public void oneIgnored() {
    expectBuildExceptionContaining("oneIgnored", "There were no executed tests: 1 suite, 1 test, 1 ignored");
  }
  
  @Test
  public void oneAssumptionIgnored() {
    expectBuildExceptionContaining("oneAssumptionIgnored", "There were no executed tests: 1 suite, 1 test, 1 ignored");
  }

  @Test
  public void oneSuccessfull() {
    executeTarget("oneSuccessful");
  }
  
  @Test
  public void oneFailure() {
    expectBuildExceptionContaining("oneFailure", "There were test failures: 1 suite, 1 test, 1 failure");
  }

  @Test
  public void oneError() {
    expectBuildExceptionContaining("oneError", "There were test failures: 1 suite, 1 test, 1 error");
  }
}

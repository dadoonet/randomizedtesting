package com.carrotsearch.ant.tasks.junit5.it;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.carrotsearch.ant.tasks.junit5.tests.FireUnhandledRunnerException;

public class TestUnhandledForkedJvmFailure extends JUnit5XmlTestBase {
  @Test
  public void checkForkedMainFailure() throws IOException {
    super.expectBuildExceptionContaining("forkedmainfailure", "process threw an exception");
    assertLogContains(FireUnhandledRunnerException.EXCEPTION_MESSAGE);
  }
}

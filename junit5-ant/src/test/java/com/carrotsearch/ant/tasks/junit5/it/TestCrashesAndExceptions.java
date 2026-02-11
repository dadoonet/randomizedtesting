package com.carrotsearch.ant.tasks.junit5.it;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

public class TestCrashesAndExceptions extends JUnit5XmlTestBase {
  @Test
  public void forkedjvmhanging() {
    executeTarget("forkedjvmhanging");
    assertLogContains("Caused by: java.lang.ArithmeticException");
  }
  

  @Test
  public void jvmcrash() {
    try {
      executeTarget("jvmcrash");
      fail("Expected a build failure.");
    } catch (BuildException e) {
      String log = getLog();
      if (log.contains("java.lang.UnsatisfiedLinkError: Could not link with crashlib")) {
        // ignore
        Assumptions.assumeTrue(false);
      }
      assertThat(e.getMessage()).contains("was not empty, see:");
    }

    File cwd = getProject().getBaseDir();
    for (File crashDump : cwd.listFiles()) {
      if (crashDump.isFile() && 
          (crashDump.getName().matches("^hs_err_pid.+\\.log") ||
           crashDump.getName().endsWith(".mdmp") ||
           crashDump.getName().endsWith(".dmp") ||
           crashDump.getName().endsWith(".dump") ||
           crashDump.getName().endsWith(".trc"))) {
        crashDump.delete();
      }
    }
  }  
}

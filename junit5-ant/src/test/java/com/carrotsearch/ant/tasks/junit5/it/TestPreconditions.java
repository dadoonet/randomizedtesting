package com.carrotsearch.ant.tasks.junit5.it;


import org.junit.Test;


public class TestPreconditions extends JUnit5XmlTestBase {
  @Test 
  public void nojunit() {
    expectBuildExceptionContaining("nojunit", "Forked JVM's classpath must include a junit5 JAR");
  }
  
  @Test 
  public void oldjunit() {
    executeForkedTarget("oldjunit");
    assertLogContains("Forked JVM's classpath must use JUnit 4.10 or newer");
  }

  @Test 
  public void nojunit_task() {
    executeForkedTarget("nojunit-task");
    assertLogContains("JUnit JAR must be added to junit5 taskdef's classpath");
  }

  @Test 
  public void oldjunit_task() {
    executeForkedTarget("oldjunit-task");
    assertLogContains("At least JUnit version 4.10 is required on junit5's taskdef classpath");
  }
}

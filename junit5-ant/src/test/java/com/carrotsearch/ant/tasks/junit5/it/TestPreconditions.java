package com.carrotsearch.ant.tasks.junit5.it;

import org.junit.jupiter.api.Test;

public class TestPreconditions extends JUnit5XmlTestBase {
  @Test 
  public void nojunit() {
    expectBuildExceptionContaining("nojunit", "Forked JVM's classpath must include a junit5 JAR");
  }
  
  @Test 
  public void oldjunit() {
    // With JUnit 5 Platform, the forked JVM check remains for junit5 JAR presence
    executeForkedTarget("oldjunit");
    assertLogContains("Forked JVM's classpath must include a junit5 JAR");
  }

  @Test 
  public void nojunit_task() {
    executeForkedTarget("nojunit-task");
    assertLogContains("JUnit 5 Platform must be added to junit5 taskdef's classpath");
  }

  @Test 
  public void oldjunit_task() {
    executeForkedTarget("oldjunit-task");
    assertLogContains("JUnit 5 Platform must be added to junit5 taskdef's classpath");
  }
}

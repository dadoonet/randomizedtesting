package com.carrotsearch.ant.tasks.junit5.it;


import org.junit.Test;


public class TestSuiteDuplicates  extends JUnit5XmlTestBase {
  @Test 
  public void suiteduplicate() {
    super.executeTarget("suiteduplicate");
    
    assertLogContains("2 suites, 2 tests");
    assertLogContains("JVM J0");
    assertLogContains("JVM J1");
  }
}

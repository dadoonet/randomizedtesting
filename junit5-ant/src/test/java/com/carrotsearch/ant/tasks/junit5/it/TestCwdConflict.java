package com.carrotsearch.ant.tasks.junit5.it;


import org.junit.Test;


public class TestCwdConflict extends JUnit5XmlTestBase {
  @Test 
  public void cwdconflict() {
    super.executeTarget("cwdconflict");
  }
}

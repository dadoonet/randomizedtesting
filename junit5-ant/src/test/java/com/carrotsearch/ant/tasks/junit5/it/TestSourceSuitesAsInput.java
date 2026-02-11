package com.carrotsearch.ant.tasks.junit5.it;

import org.junit.jupiter.api.Test;

public class TestSourceSuitesAsInput extends JUnit5XmlTestBase {
  @Test 
  public void sourcesuites() {
    super.executeTarget("sourcesuites");
    assertLogContains("Tests summary: 1 suite, 1 test");
  }
}

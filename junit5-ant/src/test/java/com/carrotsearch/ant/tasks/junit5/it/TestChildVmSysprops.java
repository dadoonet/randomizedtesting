package com.carrotsearch.ant.tasks.junit5.it;

import org.junit.jupiter.api.Test;

public class TestChildVmSysprops extends JUnit5XmlTestBase {
  @Test
  public void sysprops() {
    executeTarget("childvm_sysprops");
  }
}

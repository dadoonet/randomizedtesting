package com.carrotsearch.ant.tasks.junit5.it;

import org.junit.jupiter.api.Test;

public class TestSysoutOOM  extends JUnit5XmlTestBase {
  @Test
  public void sysoutoom() {
    super.executeTarget("sysoutoom");
  }
}

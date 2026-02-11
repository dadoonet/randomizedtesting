package com.carrotsearch.ant.tasks.junit5.it;

import org.junit.jupiter.api.Test;

public class TestStatsProperties extends JUnit5XmlTestBase {
  @Test
  public void allFilteredOut() {
    super.executeTarget("statsProperties");
  }
}

package com.carrotsearch.ant.tasks.junit5.it;

import org.junit.jupiter.api.Test;

public class TestStackOverflowInEventEmitter  extends JUnit5XmlTestBase {
  @Test
  public void stackoverflow() {
    super.expectBuildExceptionContaining("stackoverflow", "Quit event not received from the forked process?");
  }
}

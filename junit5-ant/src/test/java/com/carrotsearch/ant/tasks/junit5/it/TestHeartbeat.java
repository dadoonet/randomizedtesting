package com.carrotsearch.ant.tasks.junit5.it;


import org.junit.Test;

/**
 * Test heartbeat on slow, non-updating tests.
 */
public class TestHeartbeat extends JUnit5XmlTestBase {
  @Test
  public void testHeartbeat() {
    executeTarget("testHeartbeat");
    assertLogContains("HEARTBEAT J0");
    assertLogContains("at: HeartbeatSlow.method1");
    assertLogDoesNotContain("Exception thrown by subscriber method");
  }
}

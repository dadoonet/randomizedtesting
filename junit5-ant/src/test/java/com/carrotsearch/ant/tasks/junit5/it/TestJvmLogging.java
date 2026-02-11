package com.carrotsearch.ant.tasks.junit5.it;

import org.junit.jupiter.api.Test;

/**
 * Check JVM logging settings. They seem to use process descriptors not
 * {@link System} streams.
 */
public class TestJvmLogging extends JUnit5XmlTestBase {
  @Test
  public void jvmverbose() {
    executeTarget("jvmverbose");
    assertLogContains("was not empty, see");
  }
  
  @Test
  public void sysouts() {
    executeTarget("sysouts");
    assertLogContains("syserr-syserr-contd.");
    assertLogContains("sysout-sysout-contd.");
  }
}

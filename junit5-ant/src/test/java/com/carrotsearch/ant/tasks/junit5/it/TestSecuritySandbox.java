package com.carrotsearch.ant.tasks.junit5.it;

import org.junit.jupiter.api.Test;

/**
 * Test report-text listener.
 */
public class TestSecuritySandbox extends JUnit5XmlTestBase {
  @Test
  public void gh255() {
    super.executeTarget("gh255");
    assertLogContains("access denied (\"java.util.PropertyPermission\" \"foo\" \"write\")");
    assertLogContains("Tests summary: 1 suite, 1 test, 1 error");
  }
}

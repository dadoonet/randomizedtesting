package com.carrotsearch.ant.tasks.junit5.it;

import org.junit.jupiter.api.Test;

public class TestDuplicateResourceCollections extends JUnit5XmlTestBase {
  @Test 
  public void duplicateResourceCollectionEntries() {
    super.executeTarget("duplicateresources");

    assertLogContains("10 suites, 10 tests");
    assertLogContains("JVM J0");
    assertLogContains("JVM J1");
  }
}

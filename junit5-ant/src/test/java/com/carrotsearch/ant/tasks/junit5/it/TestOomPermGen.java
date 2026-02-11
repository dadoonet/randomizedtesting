package com.carrotsearch.ant.tasks.junit5.it;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TestOomPermGen  extends JUnit5XmlTestBase {
  @Test
  @Disabled("Not portable across JVMs")
  public void oom() {
    super.executeForkedTarget("oompermgen", 5 * 60 * 1000L);
    if (!getLog().contains("1 ignored (1 assumption)")) {
      assertLogContains("java.lang.OutOfMemoryError");
    }
  }
}

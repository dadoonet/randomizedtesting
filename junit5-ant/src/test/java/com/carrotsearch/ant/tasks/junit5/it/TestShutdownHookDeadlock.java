package com.carrotsearch.ant.tasks.junit5.it;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

public class TestShutdownHookDeadlock extends JUnit5XmlTestBase {
  @Test
  public void forkedjvmhanging() {
    long start = System.nanoTime();
    executeForkedTarget("shutdownhook", 120 * 1000L);
    long end = System.nanoTime();

    // This isn't a strong assertion but it'll do here. If the execution time > 60 seconds
    // something is stinky.
    assertThat(TimeUnit.NANOSECONDS.toMillis(end - start)).isLessThan(60 * 1000);
  }
}

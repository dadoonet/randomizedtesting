package com.carrotsearch.ant.tasks.junit5.tests.sub1;

import org.junit.Test;

public class TestAssertions {
  @Test
  public void failOnAssertion() {
    assert false : "foobar";
  }
}

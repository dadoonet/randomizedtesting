package com.carrotsearch.ant.tasks.junit5.tests.replication;

import org.junit.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.ReplicateOnEachVm;

@ReplicateOnEachVm
public class TestSuiteReplicated extends RandomizedTest {
  @Test
  public void replicatedTest() {
    System.out.println("Replicated test, VM: " + System.getProperty("junit5.childvm.id"));
  }
}

package com.carrotsearch.ant.tasks.junit5.events.aggregated;

public enum TestStatus {
  OK,
  
  IGNORED,
  IGNORED_ASSUMPTION,

  FAILURE,
  ERROR,
}

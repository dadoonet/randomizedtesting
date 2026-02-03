package com.carrotsearch.ant.tasks.junit5.events;

import org.junit.runner.notification.Failure;

public class TestFailureEvent extends FailureEvent {
  protected TestFailureEvent() {
    super(EventType.TEST_FAILURE);
  }

  public TestFailureEvent(Failure failure) {
    this();
    setFailure(failure);
  }
}

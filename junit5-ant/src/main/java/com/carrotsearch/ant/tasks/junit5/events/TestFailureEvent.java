package com.carrotsearch.ant.tasks.junit5.events;

import com.carrotsearch.ant.tasks.junit5.events.mirrors.FailureMirror;
import com.carrotsearch.ant.tasks.junit5.events.mirrors.TestDescriptionMirror;

public class TestFailureEvent extends FailureEvent {
  protected TestFailureEvent() {
    super(EventType.TEST_FAILURE);
  }

  public TestFailureEvent(FailureMirror failure) {
    this();
    setFailure(failure);
  }

  public TestFailureEvent(TestDescriptionMirror description, Throwable cause) {
    this();
    setFailure(description, cause);
  }
}

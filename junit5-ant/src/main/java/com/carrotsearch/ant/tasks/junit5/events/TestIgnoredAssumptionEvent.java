package com.carrotsearch.ant.tasks.junit5.events;

import com.carrotsearch.ant.tasks.junit5.events.mirrors.FailureMirror;
import com.carrotsearch.ant.tasks.junit5.events.mirrors.TestDescriptionMirror;

public class TestIgnoredAssumptionEvent extends FailureEvent {
  protected TestIgnoredAssumptionEvent() {
    super(EventType.TEST_IGNORED_ASSUMPTION);
  }

  public TestIgnoredAssumptionEvent(FailureMirror failure) {
    this();
    setFailure(failure);
  }

  public TestIgnoredAssumptionEvent(TestDescriptionMirror description, Throwable cause) {
    this();
    setFailure(description, cause);
  }
}

package com.carrotsearch.ant.tasks.junit5.events;

import com.carrotsearch.ant.tasks.junit5.events.mirrors.FailureMirror;
import com.carrotsearch.ant.tasks.junit5.events.mirrors.TestDescriptionMirror;

/**
 * Suite failure event.
 */
public class SuiteFailureEvent extends FailureEvent {
  protected SuiteFailureEvent() {
    super(EventType.SUITE_FAILURE);
  }

  public SuiteFailureEvent(FailureMirror failure) {
    this();
    setFailure(failure);
  }

  public SuiteFailureEvent(TestDescriptionMirror description, Throwable cause) {
    this();
    setFailure(description, cause);
  }
}

package com.carrotsearch.ant.tasks.junit5.events;

import com.carrotsearch.ant.tasks.junit5.events.mirrors.TestDescriptionMirror;

public class TestStartedEvent extends AbstractEventWithDescription {
  protected TestStartedEvent() {
    super(EventType.TEST_STARTED);
  }

  public TestStartedEvent(TestDescriptionMirror description) {
    this();
    setDescription(description);
  }
}

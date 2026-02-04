package com.carrotsearch.ant.tasks.junit5.events.aggregated;

import java.util.List;

import com.carrotsearch.ant.tasks.junit5.ForkedJvmInfo;
import com.carrotsearch.ant.tasks.junit5.events.IEvent;
import com.carrotsearch.ant.tasks.junit5.events.mirrors.FailureMirror;
import com.carrotsearch.ant.tasks.junit5.events.mirrors.TestDescriptionMirror;

/**
 * Aggregated result from a suite or test.
 */
public interface AggregatedResultEvent {
  public TestDescriptionMirror getDescription();
  public ForkedJvmInfo getForkedJvmInfo();
  public boolean isSuccessful();
  public List<FailureMirror> getFailures();
  List<IEvent> getEventStream();  
  
  public long getStartTimestamp();
  public long getExecutionTime();
}

package com.carrotsearch.ant.tasks.junit5.events.aggregated;

import java.util.List;

import org.junit.runner.Description;

import com.carrotsearch.ant.tasks.junit5.ForkedJvmInfo;
import com.carrotsearch.ant.tasks.junit5.events.IEvent;
import com.carrotsearch.ant.tasks.junit5.events.mirrors.FailureMirror;

/**
 * Aggregated result from a suite or test.
 */
public interface AggregatedResultEvent {
  public Description getDescription();
  public ForkedJvmInfo getForkedJvmInfo();
  public boolean isSuccessful();
  public List<FailureMirror> getFailures();
  List<IEvent> getEventStream();  
  
  public long getStartTimestamp();
  public long getExecutionTime();
}

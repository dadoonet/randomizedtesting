package com.carrotsearch.ant.tasks.junit5.listeners;

import org.apache.tools.ant.BuildException;

import com.carrotsearch.ant.tasks.junit5.JUnit5;
import com.carrotsearch.ant.tasks.junit5.events.aggregated.*;
import com.google.common.eventbus.EventBus;

/**
 * A dummy interface to indicate listener types for ANT. {@link JUnit5} uses
 * guava's {@link EventBus} to propagate events to listeners.
 * 
 * @see AggregatedSuiteResultEvent
 * @see AggregatedTestResultEvent
 * @see AggregatedQuitEvent
 */
public interface AggregatedEventListener {
  /**
   * Link to the container. Listener can throw {@link BuildException} if
   * parameter validation doesn't succeed, for example.
   */
  void setOuter(JUnit5 junit) throws BuildException;
}

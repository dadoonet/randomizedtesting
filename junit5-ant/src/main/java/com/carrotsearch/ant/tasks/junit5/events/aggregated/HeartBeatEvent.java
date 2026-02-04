package com.carrotsearch.ant.tasks.junit5.events.aggregated;

import com.carrotsearch.ant.tasks.junit5.ForkedJvmInfo;
import com.carrotsearch.ant.tasks.junit5.events.mirrors.TestDescriptionMirror;

/**
 * High level heartbeat event issued to report listeners when a forked JVM
 * does not repond for a longer while. The {@link #getDescription} method should
 * return an approximate place where the forked JVM is at the moment, but this is
 * not guaranteed (and may be null).
 */
public final class HeartBeatEvent {
  private final ForkedJvmInfo forkedJvmInfo;
  private final TestDescriptionMirror description;
  private final long lastActivity;
  private final long currentTime;

  public HeartBeatEvent(ForkedJvmInfo forkedJvmInfo, TestDescriptionMirror description, long lastActivity, long currentTime) {
    this.forkedJvmInfo = forkedJvmInfo;
    this.description = description;
    this.lastActivity = lastActivity;
    this.currentTime = currentTime;
  }
  
  public TestDescriptionMirror getDescription() {
    return description;
  }
  
  public long getCurrentTime() {
    return currentTime;
  }
  
  public long getLastActivity() {
    return lastActivity;
  }

  public long getNoEventDuration() {
    return getCurrentTime() - getLastActivity();
  }

  public ForkedJvmInfo getForkedJvmInfo() {
    return forkedJvmInfo;
  }
}

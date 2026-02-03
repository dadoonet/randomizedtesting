package com.carrotsearch.ant.tasks.junit5.events;

/**
 * Heartbeat for reporting long running tests.
 */
public class LowLevelHeartBeatEvent {
  public final long lastActivity, currentTime;
  
  public LowLevelHeartBeatEvent(long last, long currentTime) {
    this.lastActivity = last;
    this.currentTime = currentTime;
  }
}

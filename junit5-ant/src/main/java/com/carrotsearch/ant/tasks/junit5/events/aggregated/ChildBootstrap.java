package com.carrotsearch.ant.tasks.junit5.events.aggregated;

import com.carrotsearch.ant.tasks.junit5.ForkedJvmInfo;

public class ChildBootstrap {
  public final ForkedJvmInfo childInfo;

  public ChildBootstrap(ForkedJvmInfo childInfo) {
    this.childInfo = childInfo;
  }
  
  public ForkedJvmInfo getForkedJvmInfo() {
    return childInfo;
  }
}

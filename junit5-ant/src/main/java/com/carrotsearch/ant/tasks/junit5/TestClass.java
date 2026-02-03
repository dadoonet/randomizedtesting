package com.carrotsearch.ant.tasks.junit5;

final class TestClass {
  String className;
  boolean replicate;

  public TestClass() {
    this(null);
  }

  public TestClass(String className) {
    this.className = className;
  }
}

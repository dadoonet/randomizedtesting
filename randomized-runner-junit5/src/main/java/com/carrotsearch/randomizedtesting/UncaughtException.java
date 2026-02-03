package com.carrotsearch.randomizedtesting;

/**
 * Represents an uncaught exception from a thread.
 */
public class UncaughtException {
  final Thread thread;
  final String threadName;
  final Throwable error;

  UncaughtException(Thread t, Throwable error) {
    this.thread = t;
    this.threadName = Threads.threadName(t);
    this.error = error;
  }
}

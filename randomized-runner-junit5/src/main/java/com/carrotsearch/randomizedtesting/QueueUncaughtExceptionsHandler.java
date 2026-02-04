package com.carrotsearch.randomizedtesting;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Queue uncaught exceptions for later processing.
 */
public class QueueUncaughtExceptionsHandler implements UncaughtExceptionHandler {
  private static final Logger logger = Logger.getLogger(QueueUncaughtExceptionsHandler.class.getName());
  
  private final ArrayList<UncaughtException> uncaughtExceptions = new ArrayList<>();
  private boolean reporting = true;

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    synchronized (uncaughtExceptions) {
      if (reporting) {
        logger.log(Level.SEVERE, "Uncaught exception in thread: " + t.getName(), e);
        uncaughtExceptions.add(new UncaughtException(t, e));
      }
    }
  }

  /**
   * Stop reporting uncaught exceptions (for cleanup operations).
   */
  public void stopReporting() {
    synchronized (uncaughtExceptions) {
      reporting = false;
    }
  }

  /**
   * Resume reporting uncaught exceptions.
   */
  public void resumeReporting() {
    synchronized (uncaughtExceptions) {
      reporting = true;
    }
  }

  /**
   * Get and clear all uncaught exceptions.
   */
  public List<UncaughtException> getUncaughtAndClear() {
    synchronized (uncaughtExceptions) {
      ArrayList<UncaughtException> copy = new ArrayList<>(uncaughtExceptions);
      uncaughtExceptions.clear();
      return copy;
    }
  }
}

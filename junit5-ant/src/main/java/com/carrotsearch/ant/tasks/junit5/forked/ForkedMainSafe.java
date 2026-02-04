package com.carrotsearch.ant.tasks.junit5.forked;

import com.carrotsearch.randomizedtesting.annotations.SuppressForbidden;

@SuppressForbidden("legitimate sysstreams.")
public class ForkedMainSafe {
  public static void main(String[] args) {
    verifyJUnit5Present();

    try {
      ForkedMain.main(args);
    } catch (Throwable e) {
      try  {
        System.err.println(e.toString());
        e.printStackTrace(System.err);
        System.out.close();
        System.err.close();
      } finally {
        JvmExit.halt(ForkedMain.ERR_EXCEPTION);
      }
    }
  }

  /**
   * Verify JUnit 5 Platform presence.
   */
  private static void verifyJUnit5Present() {
    try {
      // Verify JUnit Platform Launcher is present
      Class.forName("org.junit.platform.launcher.Launcher");
      // Verify JUnit Jupiter Engine is present
      Class.forName("org.junit.jupiter.engine.JupiterTestEngine");
    } catch (ClassNotFoundException e) {
      System.err.println("JUnit 5 Platform not found on classpath: " + e.getMessage());
      JvmExit.halt(ForkedMain.ERR_NO_JUNIT);
    }
  }  
}

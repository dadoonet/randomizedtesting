package com.carrotsearch.randomizedtesting;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestOverridingDefaultExceptionHandler extends WithNestedTestClass {
  static List<String> throwableMessages = new CopyOnWriteArrayList<>();

  @SuppressWarnings("serial")
  public static class TestException extends RuntimeException {
    public TestException(String msg) {
      super(msg);
    }
  }

  public static class Nested extends RandomizedTest {
    private static UncaughtExceptionHandler defaultHandler;

    @BeforeAll
    public static void beforeClass() {
      defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
      Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
        public void uncaughtException(Thread t, Throwable e) {
          if (e instanceof TestException) {
            throwableMessages.add(e.getMessage());
          } else {
            defaultHandler.uncaughtException(t,  e);
          }
        }
      });
    }
    
    @AfterAll
    public static void afterClass() {
      Thread.setDefaultUncaughtExceptionHandler(defaultHandler);
    }

    @Test
    public void exceptionFromChildThread() throws Exception {
      Thread t = new Thread() {
        @Override
        public void run() {
          throw new TestException("exceptionFromChildThread");
        }
      };
      t.start();
      t.join();
    }

    @Test
    public void exceptionFromSubGroup() throws Exception {
      ThreadGroup subgroup = new ThreadGroup("subgroup");
      Thread t = new Thread(subgroup, new Runnable() {
        @Override
        public void run() {
          throw new TestException("exceptionFromSubGroup");
        }
      });
      t.start();
      t.join();
    }    
  }

  @Test
  public void testHandlerPropagation() {
    runTests(Nested.class);

    Assertions.assertThat(throwableMessages).contains("exceptionFromChildThread");
    Assertions.assertThat(throwableMessages).contains("exceptionFromSubGroup");
  }
}

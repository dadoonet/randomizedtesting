package com.carrotsearch.ant.tasks.junit5;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.RandomizedExtension;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.Repeat;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakLingering;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope.Scope;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(RandomizedExtension.class)
@ThreadLeakScope(Scope.SUITE)
@ThreadLeakLingering(linger = 1000)
public class TestEventBusSanityCheck extends RandomizedTest {
  static class ForkedJvmIdle {
    public void finished() {
      sleep(randomIntBetween(1, 50));
    }
    
    public void newSuite(String suiteName) {
      sleep(randomIntBetween(1, 2));
    }
  }
  
  @Test
  @Repeat(iterations = 100)
  public void testArrayQueueReentrance() throws Exception {
    // Mockups.
    final List<String> foo = new ArrayList<>();
    for (int i = randomIntBetween(2, 1000); --i > 0;) {
      foo.add(randomAsciiLettersOfLength(20));
    }
    final EventBus aggregatedBus = new EventBus("aggregated");

    final AtomicBoolean hadErrors = new AtomicBoolean();
    
    // Code mirrors JUnit5's behavior.
    ExecutorService executor = Executors.newFixedThreadPool(2);
    final Deque<ForkedJvmIdle> idle = new ArrayDeque<>();
    final ForkedJvmIdle e1 = new ForkedJvmIdle();
    final ForkedJvmIdle e2 = new ForkedJvmIdle();
    idle.addFirst(e1);
    idle.addFirst(e2);

    class ExceptionListener {
      @Subscribe
      public void onThrowable(Throwable t) {
        hadErrors.set(true);
        t.printStackTrace();
      }
    }

    class SuiteListener {
      @Subscribe
      public void onSuiteStarted(String e) {
        ForkedJvmIdle forkedJvm = idle.pollFirst();
        sleep(randomIntBetween(1, 10));
        forkedJvm.newSuite(e);
      }
    }
    
    class RestartOnIdle {
      @Subscribe
      public void onIdle(ForkedJvmIdle e) {
        sleep(randomIntBetween(1, 10));
        idle.addLast(e);
      }
    }

    List<Future<Void>> futures = new ArrayList<>();
    aggregatedBus.register(new ExceptionListener());
    aggregatedBus.register(new SuiteListener());
    aggregatedBus.register(new RestartOnIdle());
    for (final String suite : foo) {
      Callable<Void> c = new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          aggregatedBus.post(suite);
          aggregatedBus.post(idle.pollFirst());
          return null;
        }
      };
      futures.add(executor.submit(c));
    }

    for (Future<Void> f : futures) {
      f.get();
    }
    
    assertFalse(hadErrors.get());
  }
}

package com.carrotsearch.randomizedtesting.timeouts;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import com.carrotsearch.randomizedtesting.RandomizedExtension;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.WithNestedTestClass;
import com.carrotsearch.randomizedtesting.annotations.Repeat;
import com.carrotsearch.randomizedtesting.annotations.Seed;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope.Scope;
import com.carrotsearch.randomizedtesting.annotations.Timeout;
import com.carrotsearch.randomizedtesting.extensions.RepeatExtension;

/**
 * It should not matter for the random sequence whether {@link Timeout} is
 * used or not.
 */
public class Test009TimeoutOrNotIdenticalSequence extends WithNestedTestClass {
  final static ArrayList<String> seeds = new ArrayList<String>();

  @ExtendWith(RandomizedExtension.class)
  @ThreadLeakScope(Scope.NONE)
  public static class Nested1 extends RandomizedTest {
    @TestTemplate
    @ExtendWith(RepeatExtension.class)
    @Seed("deadbeef")
    @Repeat(iterations = 2, useConstantSeed = false)
    public void testNoTimeout() {
      assumeRunningNested();
      seeds.add(randomAsciiLettersOfLength(20));
    }
  }
  
  @ExtendWith(RandomizedExtension.class)
  @ThreadLeakScope(Scope.NONE)
  public static class Nested2 extends RandomizedTest {
    @TestTemplate
    @ExtendWith(RepeatExtension.class)
    @Seed("deadbeef")
    @Repeat(iterations = 2, useConstantSeed = false)
    @Timeout(millis = 30 * 1000)
    public void testNoTimeout() {
      assumeRunningNested();
      seeds.add(randomAsciiLettersOfLength(20));
    }
  }

  @ExtendWith(RandomizedExtension.class)
  @Timeout(millis = 30 * 1000)
  @ThreadLeakScope(Scope.NONE)
  public static class Nested3 extends RandomizedTest {
    @TestTemplate
    @ExtendWith(RepeatExtension.class)
    @Seed("deadbeef")
    @Repeat(iterations = 2, useConstantSeed = false)
    @Timeout(millis = 30 * 1000)
    public void testNoTimeout() {
      assumeRunningNested();
      seeds.add(randomAsciiLettersOfLength(20));
    }
  }

  @Test
  public void checkAllRunsIdentical() {
    List<String> previous = null;
    for (Class<?> c : new Class<?> [] {Nested1.class, Nested2.class, Nested3.class}) {
      seeds.clear();
      Assertions.assertThat(runTests(c).wasSuccessful()).isTrue();

      if (previous != null) {
        Assertions.assertThat(seeds).as("Class " + c.getSimpleName()).isEqualTo(previous);
      } else {
        previous = new ArrayList<>(seeds); 
      }
    }
  }
}

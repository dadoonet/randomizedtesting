package com.carrotsearch.ant.tasks.junit5.events;

import com.carrotsearch.ant.tasks.junit5.events.mirrors.TestDescriptionMirror;

/**
 * An event that carries a {@link TestDescriptionMirror}.
 */
public interface IDescribable {
  /**
   * Returns the test description mirror containing essential test information.
   */
  TestDescriptionMirror getDescription();
}

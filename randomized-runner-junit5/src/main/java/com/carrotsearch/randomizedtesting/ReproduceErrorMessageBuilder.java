package com.carrotsearch.randomizedtesting;

import java.util.Arrays;

import org.junit.platform.launcher.TestIdentifier;

import static com.carrotsearch.randomizedtesting.SysGlobals.*;
import static com.carrotsearch.randomizedtesting.RandomizedRunnerConstants.*;

// TODO: [GH-212]: how to provide better reproduce messages (especially with
// external runner tasks that provide system properties, jvm options, etc?)

/**
 * A builder for constructing "reproduce with" message.
 * 
 * @see #appendAllOpts(TestIdentifier)
 */
public class ReproduceErrorMessageBuilder {
  private final StringBuilder b;

  public ReproduceErrorMessageBuilder() {
    this(new StringBuilder());
  }

  public ReproduceErrorMessageBuilder(StringBuilder builder) {
    this.b = builder;
  }

  /**
   * Append all JVM options that may help in reproducing the error. Options are
   * appended to the provided StringBuilder in the "command-line" syntax of:
   * <pre>
   * -Doption="value"
   * </pre>
   * 
   * @param testIdentifier Test identifier.
   */
  public ReproduceErrorMessageBuilder appendAllOpts(TestIdentifier testIdentifier) {
    RandomizedContext ctx = null;
    try {
      ctx = RandomizedContext.current();
      appendOpt(SYSPROP_RANDOM_SEED(), ctx.getRunnerSeedAsString());
    } catch (IllegalStateException e) {
      logger.warning("No context available when dumping reproduce options?");
    }

    testIdentifier.getSource().ifPresent(source -> {
      if (source instanceof org.junit.platform.engine.support.descriptor.ClassSource) {
        org.junit.platform.engine.support.descriptor.ClassSource classSource = 
            (org.junit.platform.engine.support.descriptor.ClassSource) source;
        appendOpt(SYSPROP_TESTCLASS(), classSource.getClassName());
      } else if (source instanceof org.junit.platform.engine.support.descriptor.MethodSource) {
        org.junit.platform.engine.support.descriptor.MethodSource methodSource = 
            (org.junit.platform.engine.support.descriptor.MethodSource) source;
        appendOpt(SYSPROP_TESTCLASS(), methodSource.getClassName());
        appendOpt(SYSPROP_TESTMETHOD(), methodSource.getMethodName());
      }
    });

    appendRunnerProperties();
    appendTestGroupOptions(ctx);
    appendEnvironmentSettings();

    return this;
  }

  public ReproduceErrorMessageBuilder appendEnvironmentSettings() {
    for (String sysPropName : Arrays.asList(
        "file.encoding", "user.timezone")) {
      if (emptyToNull(System.getProperty(sysPropName)) != null) {
        appendOpt(sysPropName, System.getProperty(sysPropName));
      }
    }
    return this;
  }

  public ReproduceErrorMessageBuilder appendTestGroupOptions(RandomizedContext ctx) {
    if (ctx != null) {
      ctx.getGroupEvaluator().appendGroupFilteringOptions(this);
    }
    return this;
  }

  public ReproduceErrorMessageBuilder appendRunnerProperties() {
    appendOpt(SYSPROP_PREFIX(), CURRENT_PREFIX());
    for (String sysPropName : Arrays.asList(
        SYSPROP_STACKFILTERING(),
        SYSPROP_ITERATIONS(),
        SYSPROP_KILLATTEMPTS(),
        SYSPROP_KILLWAIT(),
        SYSPROP_TIMEOUT())) {
      if (System.getProperty(sysPropName) != null) {
        appendOpt(sysPropName, System.getProperty(sysPropName));
      }
    }
    return this;
  }

  /**
   * Append a single VM option.
   */
  public ReproduceErrorMessageBuilder appendOpt(String sysPropName, String value) {
    if (b.length() > 0) {
      b.append(" ");
    }

    b.append("-D").append(sysPropName).append("=").append(value);
    return this;
  }
}

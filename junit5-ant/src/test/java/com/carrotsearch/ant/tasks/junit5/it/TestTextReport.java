package com.carrotsearch.ant.tasks.junit5.it;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.carrotsearch.ant.tasks.junit5.tests.FailInAfterClass;
import com.carrotsearch.ant.tasks.junit5.tests.ReasonForAssumptionIgnored;

import static org.assertj.core.api.Assertions.*;

/**
 * Test report-text listener.
 */
public class TestTextReport extends JUnit5XmlTestBase {
  @Test 
  public void suiteerror() {
    super.executeTarget("suiteerror");
    
    int count = countPattern(getLog(), FailInAfterClass.MESSAGE);
    assertThat(count).isEqualTo(1);
  }

  @Test 
  public void reasonForIgnored() {
    super.executeTarget("reasonForIgnored");
    assertLogContains("@DisabledGroup");
    assertLogContains("> Cause: Annotated @Ignore");
    assertLogContains("(Ignored method.)");
  }

  @Test 
  public void reasonForIgnoredByDisabledGroup() {
    super.executeTarget("reasonForIgnoredByDisabledGroup");
    String log = getLog();
    assertThat(log.contains("(@DisabledGroup(value=foo bar))") ||
               log.contains("(@DisabledGroup(value=\"foo bar\"))")).isTrue();
  }

  @Test 
  public void reasonForSuiteAssumptionIgnored() {
    super.executeTarget("reasonForSuiteAssumptionIgnored");

    int count = countPattern(getLog(), ReasonForAssumptionIgnored.MESSAGE);
    assertThat(count).isEqualTo(2);
  }

  @Test 
  public void listeners() {
    super.executeTarget("listeners");
    assertLogContains("testStarted: passing(com.carrotsearch.ant.tasks.junit5.tests.SuiteListeners)");
    assertLogContains("testFinished: passing(com.carrotsearch.ant.tasks.junit5.tests.SuiteListeners)");
  }

  @Test 
  public void timestamps() {
    super.executeTarget("timestamps");
    assertThat(Pattern.compile("\\[([0-9]{2}):([0-9]{2}):([0-9]{2})\\.([0-9]{3})\\]").matcher(getLog()).find())
        .as("Log should contain timestamp pattern").isTrue();
  }
  
  @Test 
  public void sysoutsOnSuiteFailure() {
    super.executeTarget("sysoutsOnSuiteFailure");
    assertLogContains("ignored-sysout");
    assertLogContains("success-sysout");
    assertLogContains("afterclass-sysout");
    assertLogContains("beforeclass-sysout");
    super.restoreSyserr.println(getLog());
  }
  
  @Test 
  public void sysoutsOnSuiteTimeout() {
    super.executeTarget("sysoutsOnSuiteTimeout");
    assertLogContains("beforeclass-sysout");
    assertLogContains("test-sysout");
    String log = getLog();
    assertThat(log.indexOf("1> test-sysout"))
        .isLessThan(log.indexOf("Suite execution timed out:"));
  }

  @Test 
  public void sysoutsPassthrough() {
    super.executeTarget("sysouts_passthrough");
  }

  @Test 
  public void failureslist() {
    super.executeTarget("failureslist");
    assertLogContains("Tests with failures");
  }

  @Test 
  public void filtertrace_default() {
    super.executeTarget("filtertrace_default");

    assertLogDoesNotContain("at sun.reflect.");
    assertLogDoesNotContain("at java.lang.reflect.Method");
    assertLogDoesNotContain("at org.junit.runners.");
    assertLogDoesNotContain("at com.carrotsearch.ant.tasks.junit5.forked.ForkedMain");
  }
  
  @Test 
  public void filtertrace_custom() {
    super.executeTarget("filtertrace_custom");

    assertLogContains("java.lang.reflect.Method");
    assertLogDoesNotContain("at org.junit.");
    assertLogDoesNotContain(".ForkedMain.");
  }
  
  @Test 
  public void errorsSoFarIndicator() {
    super.executeTarget("errorsSoFar");
    assertLogContains("Completed [1/1 (1!)]");
  }  
}

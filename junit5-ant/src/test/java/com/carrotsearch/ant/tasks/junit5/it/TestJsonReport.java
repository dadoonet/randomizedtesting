package com.carrotsearch.ant.tasks.junit5.it;

import java.io.File;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class TestJsonReport extends JUnit5XmlTestBase {
  @Test 
  public void antxml() {
    super.executeTarget("json");

    assertThat(new File(getProject().getBaseDir(), "json/report.json").length())
        .isGreaterThan(0);
    assertThat(new File(getProject().getBaseDir(), "json/report.jsonp").length())
        .isGreaterThan(0);
    assertThat(new File(getProject().getBaseDir(), "json/output.html").length())
        .isGreaterThan(0);    
  }
}

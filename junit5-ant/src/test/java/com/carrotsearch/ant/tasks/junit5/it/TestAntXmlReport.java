package com.carrotsearch.ant.tasks.junit5.it;

import static org.assertj.core.api.Assertions.*;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Order;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Persister;

public class TestAntXmlReport  extends JUnit5XmlTestBase {
  @Root(name = "failsafe-summary")
  @Order(elements = {"completed", "errors", "failures", "skipped", "failureMessage"})
  public static class MavenFailsafeSummaryModel_Local {
    @Attribute(required = false)
    public Integer result;

    @Attribute
    public boolean timeout = false;

    @Element
    public int completed;

    @Element
    public int errors;

    @Element
    public int failures;

    @Element
    public int skipped;

    @Element(required = false)
    public String failureMessage = "";
  }
  
  @Test 
  public void antxml() throws Exception {
    super.executeTarget("antxml");

    // Simple check for existence.
    assertThat(new File(getProject().getBaseDir(), "ant-xmls/TEST-com.carrotsearch.ant.tasks.junit5.tests.TestBeforeClassError.xml").length())
        .isGreaterThan(0);

    assertThat(new File(getProject().getBaseDir(), "ant-xmls/TEST-com.carrotsearch.ant.tasks.junit5.tests.replication.TestSuiteReplicated-2.xml").length())
        .isGreaterThan(0);

    // Check for warning messages about duplicate suites.
    assertLogContains("Duplicate suite name used with XML reports");
    
    // Attempt to read and parse.
    File basedir = new File(getProject().getBaseDir(), "ant-xmls");
    for (File f : basedir.listFiles()) {
      if (f.isFile() && f.getName().endsWith(".xml")) {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        docBuilder.parse(f);
      }
    }
  }
  
  @Test 
  public void summary() throws Exception {
    super.executeTarget("antxml-summary");

    Persister p = new Persister();
    File parent = getProject().getBaseDir();

    MavenFailsafeSummaryModel_Local m1 = p.read(MavenFailsafeSummaryModel_Local.class, new File(parent, "ant-xmls2/summary1.xml"));
    assertThat(m1.result).isEqualTo(255);
    assertThat(m1.completed).isEqualTo(5);
    assertThat(m1.skipped).isEqualTo(2);
    assertThat(m1.errors).isEqualTo(1);
    assertThat(m1.failures).isEqualTo(1);
    
    m1 = p.read(MavenFailsafeSummaryModel_Local.class, new File(parent, "ant-xmls2/summary2.xml"));
    assertThat(m1.result).isNull();
    assertThat(m1.completed).isEqualTo(1);
    assertThat(m1.skipped).isEqualTo(0);
    assertThat(m1.errors).isEqualTo(0);
    assertThat(m1.failures).isEqualTo(0);

    m1 = p.read(MavenFailsafeSummaryModel_Local.class, new File(parent, "ant-xmls2/summary3.xml"));
    assertThat(m1.result).isEqualTo(254);
    assertThat(m1.completed).isEqualTo(0);
    assertThat(m1.skipped).isEqualTo(0);
    assertThat(m1.errors).isEqualTo(0);
    assertThat(m1.failures).isEqualTo(0);
  }  
}

package com.carrotsearch.ant.tasks.junit5.it;

import java.io.File;

import org.apache.tools.ant.types.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class TestAltJavaVendors extends JUnit5XmlTestBase {
  @Test
  public void altVendors() {
    String altVendors = System.getProperty("alt.jvms");
    Assumptions.assumeTrue(altVendors != null && !altVendors.trim().isEmpty());

    for (String jvm : new Path(getProject(), altVendors).list()) {
      getProject().log("Trying JVM: " + jvm);
      assertThat(new File(jvm).isFile()).as("JVM is not a file: " + jvm).isTrue();
      assertThat(new File(jvm).canExecute()).as("JVM is not executable: " + jvm).isTrue();

      getProject().setProperty("jvm.exec", jvm);
      expectBuildExceptionContaining("alt-vendor", 
          "1 suite, 5 tests, 1 error, 1 failure, 2 ignored (1 assumption)");
    }
  }
}

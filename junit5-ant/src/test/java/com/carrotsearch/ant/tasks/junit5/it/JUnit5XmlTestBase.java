package com.carrotsearch.ant.tasks.junit5.it;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * A base class for tests contained in <code>junit5.xml</code>
 * file.
 */
@ExtendWith(JUnit5XmlTestBase.DumpLogOnError.class)
public class JUnit5XmlTestBase extends AntBuildFileTestBase {
  
  /**
   * Extension that dumps the Ant log on test failure.
   */
  public static class DumpLogOnError implements TestWatcher {
    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
      // Get the test instance to access the log
      context.getTestInstance().ifPresent(instance -> {
        if (instance instanceof JUnit5XmlTestBase) {
          try {
            System.out.println("Ant log: " + ((JUnit5XmlTestBase) instance).getLog());
          } catch (Exception e) {
            System.out.println("Could not retrieve Ant log: " + e.getMessage());
          }
        }
      });
    }
  }
  
  @BeforeEach
  public void setUp() throws Exception {
    URI resource = getClass().getClassLoader().getResource("junit5.xml").toURI();
    if (!resource.getScheme().equals("file")) {
      throw new IOException("junit5.xml not under a file URI: " + resource);
    }

    Path absolute = Paths.get(resource).toAbsolutePath();
    // System.out.println("junit5.xml at: " + absolute + (Files.exists(absolute) ? " (exists)" : " (does not exist)"));

    super.setupProject(absolute.toFile());
  }
  
  protected static int countPattern(String output, String substr) {
    int count = 0;
    for (int i = 0; i < output.length();) {
      int index = output.indexOf(substr, i);
      if (index < 0) {
        break;
      }
      count++;
      i = index + 1;
    }
    return count;
  }  
}

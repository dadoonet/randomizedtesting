package com.carrotsearch.ant.tasks.junit5.it;

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.launch.Launcher;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.LoaderUtils;
import org.junit.jupiter.api.AfterEach;

/**
 * An equivalent of <code>BuildFileTest</code> for JUnit5.
 */
public class AntBuildFileTestBase {
  private Project project;
  private ByteArrayOutputStream output;
  private DefaultLogger listener;

  protected PrintStream restoreSysout;
  protected PrintStream restoreSyserr;

  protected void setupProject(File projectFile) {
    try {
      restoreSysout = System.out;
      restoreSyserr = System.err;
      output = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(output, true, "UTF-8");
      System.setOut(ps);
      System.setErr(ps);

      project = new Project();
      project.init();

      project.setUserProperty(MagicNames.ANT_FILE, projectFile.getAbsolutePath());
      ProjectHelper.configureProject(project, projectFile);

      listener = new DefaultLogger();
      listener.setMessageOutputLevel(Project.MSG_DEBUG);
      listener.setErrorPrintStream(ps);
      listener.setOutputPrintStream(ps);
      getProject().addBuildListener(listener);

      DefaultLogger console = new DefaultLogger();
      console.setMessageOutputLevel(Project.MSG_INFO);
      console.setErrorPrintStream(restoreSyserr);
      console.setOutputPrintStream(restoreSysout);
      getProject().addBuildListener(console);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @AfterEach
  public void restoreSysouts() {
    if (restoreSysout != null) System.setOut(restoreSysout);
    if (restoreSyserr != null) System.setErr(restoreSyserr);
  }

  protected final Project getProject() {
    if (project == null) {
      throw new IllegalStateException(
          "Setup project file with setupProject(File) first.");
    }
    return project;
  }
  
  protected final void assertLogContains(String substring) {
    assertThat(getLog()).as("Log should contain: '%s'", substring).contains(substring);
  }

  protected final void assertLogDoesNotContain(String substring) {
    assertThat(getLog()).as("Log should not contain: '%s'", substring).doesNotContain(substring);
  }

  protected final String getLog() {
    try {
      return new String(output.toByteArray(), "UTF-8");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected final void expectBuildExceptionContaining(String target,
      String message, String... additionalMessages) {
        try {
          executeTarget(target);
          fail("Expected a build failure with message: " + message);
        } catch (BuildException e) {
          assertThat(e.getMessage()).contains(message);
          for (String m : additionalMessages) {
            assertThat(e.getMessage()).contains(m);
          }
        }
      }

  protected final void executeTarget(String target) {
    getProject().executeTarget(target);
  }

  protected final void executeForkedTarget(String target) {
    executeForkedTarget(target, 10 * 1000L);
  }

  protected final void executeForkedTarget(String target, long timeout) {
    Path antPath = new Path(getProject());
    antPath.createPathElement().setLocation(sourceOf(Project.class));
    antPath.createPathElement().setLocation(sourceOf(Launcher.class));

    Java java = new Java();
    java.setTaskName("forked");
    java.setProject(getProject());
    java.setClassname("org.apache.tools.ant.launch.Launcher");
    java.createClasspath().add(antPath);
    java.setFork(true);
    java.setSpawn(false);
    java.setTimeout(timeout);
    java.setFailonerror(false);
    java.setOutputproperty("stdout");
    java.setErrorProperty("stderr");

    java.createArg().setValue("-f");
    java.createArg().setValue(getProject().getUserProperty(MagicNames.ANT_FILE));
    java.createArg().setValue(target);
    java.execute();    

    getProject().log("Forked stdout:\n" + getProject().getProperty("stdout"));
    getProject().log("Forked stderr:\n" + getProject().getProperty("stderr"));
  }
  
  /** 
   * Get the source location of a given class.
   */
  private static File sourceOf(Class<?> clazz) {
    return LoaderUtils.getResourceSource(
        clazz.getClassLoader(), clazz.getName().replace('.', '/') + ".class");
  }  
}

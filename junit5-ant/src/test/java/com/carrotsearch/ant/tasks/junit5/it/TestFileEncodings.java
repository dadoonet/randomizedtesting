package com.carrotsearch.ant.tasks.junit5.it;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class TestFileEncodings extends JUnit5XmlTestBase {
  @Test
  public void checkTypicalEncodings() throws IOException {
    super.executeTarget("fileencodings");

    File logFile = new File(getProject().getProperty("log.file"));

    byte [] contents = new byte [(int) logFile.length()];
    DataInputStream dis = new DataInputStream(new FileInputStream(logFile));
    dis.readFully(contents);
    dis.close();
    
    String log = new String(contents, "UTF-8");
    assertThat(countPattern(log, "US-ASCII=cze??, ??????, ???")).isEqualTo(1);
    assertThat(countPattern(log, "iso8859-1=cze??, ??????, ???")).isEqualTo(1);
    assertThat(countPattern(log, "UTF-8=cześć, Привет, 今日は")).isEqualTo(1);
    assertThat(countPattern(log, "UTF-16=cześć, Привет, 今日は")).isEqualTo(1);
    assertThat(countPattern(log, "UTF-16LE=cześć, Привет, 今日は")).isEqualTo(1);
    assertThat(countPattern(log, "UTF-32=cześć, Привет, 今日は")).isEqualTo(1);
  }
}

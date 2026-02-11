package com.carrotsearch.ant.tasks.junit5.it;

import java.util.HashMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class TestPickSeed extends JUnit5XmlTestBase {
  
  @Test
  public void pickSeed() {
    for (int i = 0; i < 7; i++) {
      super.executeTarget("randomsysproperty");
    }

    HashMap<String, TreeSet<String>> props = new HashMap<String, TreeSet<String>>();  

    for (String key : new String [] {
        "prefix.dummy1",
        "prefix.dummy2",
        "replaced.dummy1",
        "replaced.dummy2",
    }) {
      TreeSet<String> values = new TreeSet<String>();
      Pattern p = Pattern.compile("(?:> )(" + key + ")(?:=)([^\\s]+)");
      Matcher m = p.matcher(getLog());
      while (m.find()) {
        values.add(m.group(2));
      }
      
      props.put(key, values);
    }
    
    assertThat(props.get("prefix.dummy1")).isEqualTo(props.get("replaced.dummy1"));
    assertThat(props.get("prefix.dummy2")).isEqualTo(props.get("replaced.dummy2"));

    // At least two unique values.
    assertThat(props.get("prefix.dummy1").size()).isGreaterThanOrEqualTo(2);
    // null (missing value) should be there.
    assertThat(props.get("prefix.dummy2")).contains("null");
  }
}

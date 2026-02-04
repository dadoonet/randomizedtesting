package com.carrotsearch.randomizedtesting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class TestGlobFilter {
  @Test
  public void testBrackets() {
    GlobFilter gf = new MethodGlobFilter("ab(*)");
    assertThat(gf.globMatches("ab()")).isTrue();
    assertThat(gf.globMatches("ab(foo)")).isTrue();
    assertThat(gf.globMatches("ab(bar=xxx)")).isTrue();

    gf = new MethodGlobFilter("test {yaml=resthandler/10_Foo/Bar (Hello)}");
    assertThat(gf.globMatches("test {yaml=resthandler/10_Foo/Bar (Hello)}")).isTrue();
  }
}

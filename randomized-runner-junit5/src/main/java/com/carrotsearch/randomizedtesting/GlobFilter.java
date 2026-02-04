package com.carrotsearch.randomizedtesting;

import java.util.regex.Pattern;

/**
 * A filter that matches something using globbing (*) pattern.
 * This is the JUnit 5 version that doesn't depend on JUnit 4's Filter class.
 */
public abstract class GlobFilter {
  protected final String globPattern;
  private final Pattern pattern;

  public GlobFilter(String glob) {
    this.globPattern = glob;
    this.pattern = globToPattern(glob);
  }

  /**
   * Check if a given string matches the glob.
   */
  public final boolean globMatches(String string) {
    return pattern.matcher(string).matches();
  }

  /**
   * Simplified conversion to a regexp.
   */
  private Pattern globToPattern(String glob) {
    StringBuilder pattern = new StringBuilder("^");
    for (char c : glob.toCharArray()) {
      switch (c) {
        case '*':
          pattern.append(".*");
          break;
        case '?':
          pattern.append('.');
          break;
        case '.':
        case '$':
        case '-':
        case '{':
        case '}':
        case '[':
        case ']':
        case '(':
        case ')':
        case '^':
        case '+':
        case '|':
          pattern.append("\\");
          pattern.append(c);
          break;
        case '\\':
          pattern.append("\\\\");
          break;
        default:
          pattern.append(c);
      }
    }
    pattern.append('$');
    return Pattern.compile(pattern.toString());
  }
  
  /**
   * Returns a description of this filter.
   */
  public abstract String describe();
}

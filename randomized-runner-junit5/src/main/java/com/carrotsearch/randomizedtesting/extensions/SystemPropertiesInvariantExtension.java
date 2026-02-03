package com.carrotsearch.randomizedtesting.extensions;

import java.util.*;

import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.opentest4j.AssertionFailedError;

/**
 * A JUnit 5 extension that ensures system properties remain unmodified by the test.
 * This can be applied both at suite level and at test level.
 * 
 * This extension requires appropriate security permission to read and write 
 * system properties ({@link System#getProperties()}) if running under a security
 * manager.  
 * 
 * <p>Usage:
 * <pre>
 * {@literal @}ExtendWith(SystemPropertiesInvariantExtension.class)
 * public class MyTest {
 *   // tests...
 * }
 * </pre>
 *
 * @see SystemPropertiesRestoreExtension
 */
public class SystemPropertiesInvariantExtension implements BeforeEachCallback, AfterEachCallback,
    BeforeAllCallback, AfterAllCallback {

  private static final Namespace NAMESPACE = Namespace.create(SystemPropertiesInvariantExtension.class);
  private static final String KEY_BEFORE_TEST = "beforeTest";
  private static final String KEY_BEFORE_ALL = "beforeAll";

  /**
   * Ignored property keys.
   */
  private final Set<String> ignoredProperties;

  /**
   * Cares about all properties. 
   */
  public SystemPropertiesInvariantExtension() {
    this(Collections.emptySet());
  }

  /**
   * Don't care about the given set of properties. 
   */
  public SystemPropertiesInvariantExtension(String... ignoredProperties) {
    this.ignoredProperties = new HashSet<>(Arrays.asList(ignoredProperties));
  }

  /**
   * Don't care about the given set of properties. 
   */
  public SystemPropertiesInvariantExtension(Set<String> ignoredProperties) {
    this.ignoredProperties = new HashSet<>(ignoredProperties);
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    Store store = context.getStore(NAMESPACE);
    store.put(KEY_BEFORE_ALL, SystemPropertiesRestoreExtension.systemPropertiesAsMap());
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    Store store = context.getStore(NAMESPACE);
    TreeMap<String, String> before = store.remove(KEY_BEFORE_ALL, TreeMap.class);
    if (before != null) {
      checkInvariant(before);
    }
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    Store store = context.getStore(NAMESPACE);
    store.put(KEY_BEFORE_TEST, SystemPropertiesRestoreExtension.systemPropertiesAsMap());
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    Store store = context.getStore(NAMESPACE);
    TreeMap<String, String> before = store.remove(KEY_BEFORE_TEST, TreeMap.class);
    if (before != null) {
      checkInvariant(before);
    }
  }

  private void checkInvariant(TreeMap<String, String> before) {
    TreeMap<String, String> after = SystemPropertiesRestoreExtension.systemPropertiesAsMap();

    // Remove ignored if they exist.
    TreeMap<String, String> beforeFiltered = new TreeMap<>(before);
    TreeMap<String, String> afterFiltered = new TreeMap<>(after);
    beforeFiltered.keySet().removeAll(ignoredProperties);
    afterFiltered.keySet().removeAll(ignoredProperties);

    if (!afterFiltered.equals(beforeFiltered)) {
      // Restore original properties first.
      SystemPropertiesRestoreExtension.restore(before, after, ignoredProperties);
      
      // Then throw the error.
      throw new AssertionFailedError("System properties invariant violated.\n" +
          collectErrorMessage(beforeFiltered, afterFiltered));
    }
  }

  private String collectErrorMessage(TreeMap<String, String> before, TreeMap<String, String> after) {
    TreeSet<String> newKeys = new TreeSet<>(after.keySet());
    newKeys.removeAll(before.keySet());

    TreeSet<String> missingKeys = new TreeSet<>(before.keySet());
    missingKeys.removeAll(after.keySet());

    TreeSet<String> differentKeyValues = new TreeSet<>(before.keySet());
    differentKeyValues.retainAll(after.keySet());
    differentKeyValues.removeIf(key -> {
      String valueBefore = before.get(key);
      String valueAfter = after.get(key);
      return Objects.equals(valueBefore, valueAfter);
    });

    final StringBuilder b = new StringBuilder();
    if (!missingKeys.isEmpty()) {
      b.append("Missing keys:\n");
      for (String key : missingKeys) {
        b.append("  ").append(key).append("=").append(before.get(key)).append("\n");
      }
    }
    if (!newKeys.isEmpty()) {
      b.append("New keys:\n");
      for (String key : newKeys) {
        b.append("  ").append(key).append("=").append(after.get(key)).append("\n");
      }
    }
    if (!differentKeyValues.isEmpty()) {
      b.append("Different values:\n");
      for (String key : differentKeyValues) {
        b.append("  [old]").append(key).append("=").append(before.get(key)).append("\n");
        b.append("  [new]").append(key).append("=").append(after.get(key)).append("\n");
      }
    }
    return b.toString();
  }
}

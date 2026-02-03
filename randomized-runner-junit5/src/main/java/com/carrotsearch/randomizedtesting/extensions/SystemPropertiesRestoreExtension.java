package com.carrotsearch.randomizedtesting.extensions;

import java.util.*;

import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

/**
 * A JUnit 5 extension which restores system properties after test execution.
 * 
 * This extension requires appropriate security permission to read and write 
 * system properties ({@link System#getProperties()}) if running under a security
 * manager. 
 * 
 * <p>Usage:
 * <pre>
 * {@literal @}ExtendWith(SystemPropertiesRestoreExtension.class)
 * public class MyTest {
 *   // tests...
 * }
 * </pre>
 * 
 * <p>Or use {@literal @}RegisterExtension for configuration:
 * <pre>
 * {@literal @}RegisterExtension
 * static SystemPropertiesRestoreExtension sysProps = 
 *     new SystemPropertiesRestoreExtension("my.prop", "value");
 * </pre>
 *
 * @see SystemPropertiesInvariantExtension
 */
public class SystemPropertiesRestoreExtension implements BeforeEachCallback, AfterEachCallback,
    BeforeAllCallback, AfterAllCallback {

  private static final Namespace NAMESPACE = Namespace.create(SystemPropertiesRestoreExtension.class);
  private static final String KEY_BEFORE_TEST = "beforeTest";
  private static final String KEY_BEFORE_ALL = "beforeAll";

  /**
   * Ignored property keys.
   */
  private final Set<String> ignoredProperties;

  /** properties we set up front for the duration of the test */
  private final Map<String, String> setProperties;

  /**
   * Restores all properties.
   */
  public SystemPropertiesRestoreExtension() {
    this.ignoredProperties = Collections.emptySet();
    this.setProperties = Collections.emptyMap();
  }

  /** Equivalent to calling {@link System#setProperty(String, String)} when the test starts. */
  public SystemPropertiesRestoreExtension(String key, String value) {
    this(Collections.singletonMap(key, value));
  }

  /**
   * Equivalent to calling {@link System#setProperty(String, String)} on each of the provided
   * properties when the test starts.
   */
  public SystemPropertiesRestoreExtension(Map<String, String> setProperties) {
    this.setProperties = new HashMap<>(setProperties);
    this.ignoredProperties = Collections.emptySet();
  }

  /**
   * @param ignoredProperties Properties that will be ignored (and will not be restored).
   */
  public SystemPropertiesRestoreExtension(Set<String> ignoredProperties) {
    this.ignoredProperties = new HashSet<>(ignoredProperties);
    this.setProperties = Collections.emptyMap();
  }

  /**
   * @param ignoredProperties Properties that will be ignored (and will not be restored).
   */
  public SystemPropertiesRestoreExtension(String... ignoredProperties) {
    this.ignoredProperties = new HashSet<>(Arrays.asList(ignoredProperties));
    this.setProperties = Collections.emptyMap();
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    Store store = context.getStore(NAMESPACE);
    store.put(KEY_BEFORE_ALL, systemPropertiesAsMap());
    applySetProperties();
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    Store store = context.getStore(NAMESPACE);
    TreeMap<String, String> before = store.remove(KEY_BEFORE_ALL, TreeMap.class);
    if (before != null) {
      TreeMap<String, String> after = systemPropertiesAsMap();
      if (!after.equals(before)) {
        restore(before, after, ignoredProperties);
      }
    }
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    Store store = context.getStore(NAMESPACE);
    store.put(KEY_BEFORE_TEST, systemPropertiesAsMap());
    applySetProperties();
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    Store store = context.getStore(NAMESPACE);
    TreeMap<String, String> before = store.remove(KEY_BEFORE_TEST, TreeMap.class);
    if (before != null) {
      TreeMap<String, String> after = systemPropertiesAsMap();
      if (!after.equals(before)) {
        restore(before, after, ignoredProperties);
      }
    }
  }

  private void applySetProperties() {
    for (Map.Entry<String, String> entry : setProperties.entrySet()) {
      System.setProperty(entry.getKey(), entry.getValue());
    }
  }

  private static TreeMap<String, String> cloneAsMap(Properties properties) {
    TreeMap<String, String> result = new TreeMap<>();
    for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements(); ) {
      final Object key = e.nextElement();
      if (key instanceof String) {
        String value = properties.getProperty((String) key);
        if (value == null) {
          Object ovalue = properties.get(key);
          if (ovalue != null) {
            continue;
          }
        }
        result.put((String) key, value);
      }
    }
    return result;
  }

  static void restore(
      TreeMap<String, String> before,
      TreeMap<String, String> after,
      Set<String> ignoredKeys) {

    // Clear anything that is present after but wasn't before.
    Set<String> toRemove = new TreeSet<>(after.keySet());
    toRemove.removeAll(before.keySet());
    for (String key : toRemove) {
      if (!ignoredKeys.contains(key)) {
        System.clearProperty(key);
      }
    }

    // Restore original property values unless they are ignored.
    for (Map.Entry<String, String> e : before.entrySet()) {
      String key = e.getKey();
      String value = e.getValue();
      if (!ignoredKeys.contains(key)) {
        if (value == null) {
          System.clearProperty(key);
        } else {
          System.setProperty(key, value);
        }
      }
    }
  }

  static TreeMap<String, String> systemPropertiesAsMap() {
    try {
      return cloneAsMap(System.getProperties());
    } catch (SecurityException e) {
      AssertionError ae = new AssertionError("Access to System.getProperties() denied.");
      ae.initCause(e);
      throw ae;
    }
  }
}

package com.carrotsearch.randomizedtesting.extensions;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opentest4j.AssertionFailedError;

import com.carrotsearch.randomizedtesting.rules.RamUsageEstimator;

/**
 * A JUnit 5 extension that ensures static, reference fields of the suite class
 * (and optionally its superclasses) are cleaned up after a suite is completed.
 * This is helpful in finding out static memory leaks (a class references
 * something huge but is no longer used).
 * 
 * <p>Usage:
 * <pre>
 * {@literal @}ExtendWith(StaticFieldsInvariantExtension.class)
 * public class MyTest {
 *   // tests...
 * }
 * </pre>
 * 
 * <p>Or with configuration:
 * <pre>
 * {@literal @}RegisterExtension
 * static StaticFieldsInvariantExtension staticFields = 
 *     new StaticFieldsInvariantExtension(5 * 1024 * 1024, true);
 * </pre>
 *
 * @see #accept(Field)
 */
public class StaticFieldsInvariantExtension implements AfterAllCallback {
  
  public static final long DEFAULT_LEAK_THRESHOLD = 10 * 1024 * 1024;
  
  private final long leakThreshold;
  private final boolean countSuperclasses;
  
  /**
   * By default use {@link #DEFAULT_LEAK_THRESHOLD} as the threshold and count
   * in superclasses.
   */
  public StaticFieldsInvariantExtension() {
    this(DEFAULT_LEAK_THRESHOLD, true);
  }
  
  public StaticFieldsInvariantExtension(long leakThresholdBytes, boolean countSuperclasses) {
    this.leakThreshold = leakThresholdBytes;
    this.countSuperclasses = countSuperclasses;
  }
  
  static class Entry implements Comparable<Entry> {
    final Field field;
    final Object value;
    long ramUsed;
    
    public Entry(Field field, Object value) {
      this.field = field;
      this.value = value;
    }
    
    @Override
    public int compareTo(Entry o) {
      if (this.ramUsed > o.ramUsed) return -1;
      if (this.ramUsed < o.ramUsed) return 1;
      return this.field.toString().compareTo(o.field.toString());
    }
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    Class<?> testClass = context.getRequiredTestClass();
    List<Throwable> errors = new ArrayList<>();
    
    // Collect all fields first to count references to the same object once.
    ArrayList<Entry> fieldsAndValues = new ArrayList<>();
    ArrayList<Object> values = new ArrayList<>();
    
    for (Class<?> c = testClass; countSuperclasses && c.getSuperclass() != null; c = c.getSuperclass()) {
      final Class<?> target = c;
      Field[] allFields = AccessController.doPrivileged((PrivilegedAction<Field[]>) target::getDeclaredFields);
      
      for (final Field field : allFields) {
        if (Modifier.isStatic(field.getModifiers()) && 
            !field.getType().isPrimitive() &&
            accept(field)) {
          try {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
              field.setAccessible(true);
              return null;
            });
            Object v = field.get(null);
            if (v != null) {
              fieldsAndValues.add(new Entry(field, v));
              values.add(v);
            }
          } catch (SecurityException e) {
            errors.add(new RuntimeException("Could not access field '" + field.getName() + "'.", e));
          } catch (RuntimeException e) {
            // On Java 9+, setAccessible may throw InaccessibleObjectException
            // for fields in modules that are not open. Skip such fields.
          } catch (IllegalAccessException e) {
            // Field is not accessible, skip it.
          }
        }
      }
    }

    final long ramUsage;
    try {
      ramUsage = RamUsageEstimator.sizeOfAll(values);
    } catch (Exception ex) {
      // some problem occurred while trying to measure (e.g. Java 9, SecurityManager).
      final StringBuilder b = new StringBuilder();
      b.append("Clean up static fields (in @AfterAll?) and null them, ")
        .append("your test still has references to classes of which the ")
        .append("sizes cannot be measured due to security restrictions or Java 9 ")
        .append("module encapsulation:");
      for (final Entry e : fieldsAndValues) {
        try {
          RamUsageEstimator.sizeOf(e.value);
        } catch (Exception ex1) {
          b.append("\n  - ").append(e.field);
        }
      }
      
      AssertionFailedError err = new AssertionFailedError(b.toString());
      err.initCause(ex);
      errors.add(err);
      throwIfErrors(errors);
      return;
    }
    
    if (ramUsage > leakThreshold) {
      // Count per-field information to get the heaviest fields.
      for (Entry e : fieldsAndValues) {
        e.ramUsed = RamUsageEstimator.sizeOf(e.value);
      }
      Collections.sort(fieldsAndValues);
      
      StringBuilder b = new StringBuilder();
      b.append(String.format(Locale.ROOT, "Clean up static fields (in @AfterAll?), "
          + "your test seems to hang on to approximately %,d bytes (threshold is %,d). " +
          "Field reference sizes (counted individually):",
          ramUsage, leakThreshold));

      for (Entry e : fieldsAndValues) {
        b.append(String.format(Locale.ROOT, "\n  - %,d bytes, %s", e.ramUsed,
            e.field.toString()));
      }

      errors.add(new AssertionFailedError(b.toString()));
    }
    
    throwIfErrors(errors);
  }

  private void throwIfErrors(List<Throwable> errors) throws Exception {
    if (!errors.isEmpty()) {
      if (errors.size() == 1) {
        Throwable t = errors.get(0);
        if (t instanceof Exception) {
          throw (Exception) t;
        }
        throw new RuntimeException(t);
      }
      StringBuilder sb = new StringBuilder("Multiple errors:\n");
      for (Throwable t : errors) {
        sb.append("  - ").append(t.getMessage()).append("\n");
      }
      throw new RuntimeException(sb.toString());
    }
  }

  /**
   * @return Return <code>false</code> to exclude a given field from being
   *         counted. By default final fields are rejected.
   */
  protected boolean accept(Field field) {
    return !Modifier.isFinal(field.getModifiers());
  }
}

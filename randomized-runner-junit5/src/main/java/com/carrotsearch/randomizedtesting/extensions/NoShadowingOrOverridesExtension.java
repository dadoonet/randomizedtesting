package com.carrotsearch.randomizedtesting.extensions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.carrotsearch.randomizedtesting.ClassModel;
import com.carrotsearch.randomizedtesting.ClassModel.MethodModel;

/**
 * A JUnit 5 extension that discovers shadowing or override relationships among 
 * methods annotated with any of the provided annotations.
 */
public abstract class NoShadowingOrOverridesExtension implements BeforeAllCallback {
  
  private final Class<? extends Annotation>[] annotations;

  @SafeVarargs
  public NoShadowingOrOverridesExtension(Class<? extends Annotation>... annotations) {
    this.annotations = annotations;
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    Class<?> testClass = context.getRequiredTestClass();
    validate(testClass);
  }

  public final void validate(Class<?> clazz) throws Exception {
    ClassModel classModel = new ClassModel(clazz);

    for (Class<? extends Annotation> annClass : annotations) {
      checkNoShadowsOrOverrides(clazz, classModel, annClass);
    }
  }

  private void checkNoShadowsOrOverrides(Class<?> clazz, ClassModel classModel, 
      Class<? extends Annotation> ann) throws Exception {
    Map<Method, MethodModel> annotatedLeafMethods = classModel.getAnnotatedLeafMethods(ann);

    StringBuilder b = new StringBuilder();
    for (Map.Entry<Method, MethodModel> e : annotatedLeafMethods.entrySet()) {
      if (verify(e.getKey())) {
        MethodModel mm = e.getValue();
        if (mm.getDown() != null || mm.getUp() != null) {
          b.append("Methods annotated with @").append(ann.getName())
           .append(" shadow or override each other:\n");
          while (mm.getUp() != null) {
            mm = mm.getUp();
          }
          while (mm != null) {
            b.append("  - ");
            if (mm.element.isAnnotationPresent(ann)) {
              b.append("@").append(ann.getSimpleName()).append(" ");
            }
            b.append(mm.element.toString()).append("\n");
            mm = mm.getDown();
          }
        }
      }
    }

    if (b.length() > 0) {
      throw new RuntimeException("There are overridden methods annotated with "
          + ann.getName() + ". These methods would not be executed by JUnit and need to "
          + "manually chain themselves which can lead to maintenance problems. Consider "
          + "using different method names or make hook methods private.\n" + b.toString().trim());
    }
  }

  protected boolean verify(Method key) {
    return true;
  }
}

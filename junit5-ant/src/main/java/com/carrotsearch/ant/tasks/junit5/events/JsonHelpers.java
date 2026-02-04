package com.carrotsearch.ant.tasks.junit5.events;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.carrotsearch.ant.tasks.junit5.events.mirrors.TestDescriptionMirror;
import com.carrotsearch.ant.tasks.junit5.gson.stream.JsonReader;
import com.carrotsearch.ant.tasks.junit5.gson.stream.JsonToken;
import com.carrotsearch.ant.tasks.junit5.gson.stream.JsonWriter;

public final class JsonHelpers {
  public static void writeDescription(JsonWriter writer, TestDescriptionMirror e) throws IOException {
    String key = createId(e);
    if (writer.inContext(key)) {
      writer.value(key);
    } else {
      writer.registerInContext(key, e);
      writer.beginObject();
      writer.name("id").value(key);
      writer.name("displayName").value(e.getDisplayName());
      writer.name("methodName").value(e.getMethodName());
      writer.name("className").value(e.getClassName());
  
      writer.name("children").beginArray();
      for (TestDescriptionMirror child : e.getChildren()) {
        writeDescription(writer, child);
      }
      writer.endArray();
      writer.endObject();
    }
  }

  protected static TestDescriptionMirror readDescription(JsonReader reader) throws IOException {
    final TestDescriptionMirror description;
    if (reader.peek() == JsonToken.STRING) {
      String key = reader.nextString();
      description = (TestDescriptionMirror) reader.lookupInContext(key);
      if (description == null) {
        throw new IOException("Missing reference to: " + key);
      }
    } else {
      reader.beginObject();
      String key = AbstractEvent.readStringOrNullProperty(reader, "id");
      String displayName = AbstractEvent.readStringOrNullProperty(reader, "displayName");
      String methodName = AbstractEvent.readStringOrNullProperty(reader, "methodName");
      String className = AbstractEvent.readStringOrNullProperty(reader, "className");
    
      List<TestDescriptionMirror> children = new ArrayList<>();
      AbstractEvent.expectProperty(reader, "children").beginArray();
      while (reader.peek() != JsonToken.END_ARRAY) {
        children.add(readDescription(reader));
      }
      reader.endArray();

      description = new TestDescriptionMirror(displayName, className, methodName, key, children);
      
      reader.registerInContext(key, description);
      reader.endObject();
    } 
  
    return description;
  }

  private static String createId(TestDescriptionMirror description) {
    return "ID#" + description.getDisplayName();
  }
}

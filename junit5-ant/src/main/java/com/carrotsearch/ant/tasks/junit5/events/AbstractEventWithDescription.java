package com.carrotsearch.ant.tasks.junit5.events;

import java.io.IOException;

import com.carrotsearch.ant.tasks.junit5.events.mirrors.TestDescriptionMirror;
import com.carrotsearch.ant.tasks.junit5.gson.stream.JsonReader;
import com.carrotsearch.ant.tasks.junit5.gson.stream.JsonWriter;

abstract class AbstractEventWithDescription extends AbstractEvent implements IDescribable {
  private TestDescriptionMirror description;

  public AbstractEventWithDescription(EventType type) {
    super(type);
  }

  public TestDescriptionMirror getDescription() {
    return description;
  }
  
  protected void setDescription(TestDescriptionMirror description) {
    if (this.description != null)
      throw new IllegalStateException("Initialize once.");
    this.description = description;
  }  
  
  @Override
  public void serialize(JsonWriter writer) throws IOException {
    writeDescription(writer, description);
  }

  @Override
  public void deserialize(JsonReader reader) throws IOException {
    this.description = JsonHelpers.readDescription(reader);
  }
}

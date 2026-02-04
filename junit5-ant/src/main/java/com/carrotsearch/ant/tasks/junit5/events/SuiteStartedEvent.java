package com.carrotsearch.ant.tasks.junit5.events;

import java.io.IOException;

import com.carrotsearch.ant.tasks.junit5.events.mirrors.TestDescriptionMirror;
import com.carrotsearch.ant.tasks.junit5.gson.stream.JsonReader;
import com.carrotsearch.ant.tasks.junit5.gson.stream.JsonWriter;


/**
 * Suite started event.
 */
public class SuiteStartedEvent extends AbstractEventWithDescription {
  private long startTimestamp;

  protected SuiteStartedEvent() {
    super(EventType.SUITE_STARTED);
  }

  public SuiteStartedEvent(TestDescriptionMirror description, long startTimestamp) {
    this();
    setDescription(description);
    this.startTimestamp = startTimestamp;
  }
  
  public long getStartTimestamp() {
    return startTimestamp;
  }

  @Override
  public void serialize(JsonWriter writer) throws IOException {
    writer.beginObject();
    
    writer.name("description");
    super.serialize(writer);

    writer.name("startTimestamp").value(startTimestamp);
    
    writer.endObject();
  }

  @Override
  public void deserialize(JsonReader reader) throws IOException {
    reader.beginObject();
    
    expectProperty(reader, "description");
    super.deserialize(reader);
    
    startTimestamp = readLongProperty(reader, "startTimestamp");

    reader.endObject();
  }
}
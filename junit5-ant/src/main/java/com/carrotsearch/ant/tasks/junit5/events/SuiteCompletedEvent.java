package com.carrotsearch.ant.tasks.junit5.events;

import java.io.IOException;

import com.carrotsearch.ant.tasks.junit5.events.mirrors.TestDescriptionMirror;
import com.carrotsearch.ant.tasks.junit5.gson.stream.JsonReader;
import com.carrotsearch.ant.tasks.junit5.gson.stream.JsonWriter;


/**
 * Suite completed event.
 */
public class SuiteCompletedEvent extends AbstractEventWithDescription {
  private long startTimestamp;
  private long executionTime;

  protected SuiteCompletedEvent() {
    super(EventType.SUITE_COMPLETED);
  }

  public SuiteCompletedEvent(TestDescriptionMirror description, long start, long duration) {
    this();
    this.startTimestamp = start;
    this.executionTime = duration;
    setDescription(description);
  }

  public long getExecutionTime() {
    return executionTime;
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
    writer.name("executionTime").value(executionTime);
    
    writer.endObject();
  }

  @Override
  public void deserialize(JsonReader reader) throws IOException {
    reader.beginObject();
    
    expectProperty(reader, "description");
    super.deserialize(reader);
    
    startTimestamp = readLongProperty(reader, "startTimestamp");
    executionTime =  readLongProperty(reader, "executionTime");

    reader.endObject();
  }
}

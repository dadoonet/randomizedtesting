package com.carrotsearch.ant.tasks.junit5.events;

import java.io.IOException;

import com.carrotsearch.ant.tasks.junit5.gson.stream.JsonReader;
import com.carrotsearch.ant.tasks.junit5.gson.stream.JsonWriter;

public interface RemoteEvent extends IEvent {
  void serialize(JsonWriter writer) throws IOException;
  void deserialize(JsonReader reader) throws IOException;
}

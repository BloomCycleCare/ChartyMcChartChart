package com.bloomcyclecare.cmcc.utils;

import com.bloomcyclecare.cmcc.data.entities.Entry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.joda.time.LocalDate;

import java.lang.reflect.Type;

/**
 * Created by parkeroth on 5/14/17.
 */

public class GsonUtil {

  private static final Gson INSTANCE = createInstance();

  public static Gson getGsonInstance() {
    return INSTANCE;
  }

  private static Gson createInstance() {
    return new GsonBuilder()
        .registerTypeAdapter(LocalDate.class, new LocalDateSerializer())
        .registerTypeAdapter(Entry.class, new InterfaceAdapter<Entry>())
        .create();
  }

  private static class LocalDateSerializer
      implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {

    @Override
    public JsonElement serialize(LocalDate date, Type typeOfSrc, JsonSerializationContext context) {
      String dateStr = DateUtil.toWireStr(date);
      return new JsonPrimitive(dateStr);
    }

    @Override
    public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      String jsonStr = json.getAsString();
      return DateUtil.fromWireStr(jsonStr);
    }
  }
}

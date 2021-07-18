package com.bloomcyclecare.cmcc.data.utils;

import com.bloomcyclecare.cmcc.data.models.Entry;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.joda.time.DateTime;
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
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDate.class, new LocalDateSerializer())
        .registerTypeAdapter(DateTime.class, new DateTimeSerializer())
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

  private static class DateTimeSerializer
      implements JsonSerializer<DateTime>, JsonDeserializer<DateTime> {

    @Override
    public JsonElement serialize(DateTime date, Type typeOfSrc, JsonSerializationContext context) {
      return new JsonPrimitive(date.getMillis());
    }

    @Override
    public DateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      return new DateTime(json.getAsLong());
    }
  }
}

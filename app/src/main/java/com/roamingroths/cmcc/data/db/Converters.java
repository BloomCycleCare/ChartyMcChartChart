package com.roamingroths.cmcc.data.db;

import androidx.room.TypeConverter;

import com.roamingroths.cmcc.data.domain.IntercourseTimeOfDay;
import com.roamingroths.cmcc.data.domain.Observation;
import com.roamingroths.cmcc.utils.GsonUtil;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Converters {

  @TypeConverter
  public String fromObject(Observation in) {
    return GsonUtil.getGsonInstance().toJson(in);
  }
  @TypeConverter
  public Observation toObject(String in) {
    return GsonUtil.getGsonInstance().fromJson(in, Observation.class);
  }

  private static final DateTimeFormatter DTF = DateTimeFormat.forPattern("yyyy-MM-dd");
  @TypeConverter
  public String fromLocalDate(LocalDate in) {
    return DTF.print(in);
  }
  @TypeConverter
  public LocalDate toLocalDate(String in) {
    return DTF.parseLocalDate(in);
  }

  @TypeConverter
  public IntercourseTimeOfDay toTimeOfDay(String in) {
    return IntercourseTimeOfDay.fromStr(in);
  }
  @TypeConverter
  public String fromTimeOfDay(IntercourseTimeOfDay in) {
    return in.name();
  }
}

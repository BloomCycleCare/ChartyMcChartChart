package com.bloomcyclecare.cmcc.data.db;

import com.bloomcyclecare.cmcc.data.models.instructions.BasicInstruction;
import com.bloomcyclecare.cmcc.data.models.instructions.SpecialInstruction;
import com.bloomcyclecare.cmcc.data.models.instructions.YellowStampInstruction;
import com.bloomcyclecare.cmcc.data.models.measurement.LHTestResult;
import com.bloomcyclecare.cmcc.data.models.measurement.MonitorReading;
import com.bloomcyclecare.cmcc.data.models.medication.Medication;
import com.bloomcyclecare.cmcc.data.models.medication.Prescription;
import com.bloomcyclecare.cmcc.data.models.observation.IntercourseTimeOfDay;
import com.bloomcyclecare.cmcc.data.models.observation.Observation;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelection;
import com.bloomcyclecare.cmcc.data.utils.GsonUtil;
import com.bloomcyclecare.cmcc.utils.BoolMapping;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.google.gson.reflect.TypeToken;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.room.TypeConverter;

public class Converters {

  @TypeConverter
  public String fromObject(Observation in) {
    return GsonUtil.getGsonInstance().toJson(in);
  }
  @TypeConverter
  public Observation toObject(String in) {
    return GsonUtil.getGsonInstance().fromJson(in, Observation.class);
  }

  @TypeConverter
  public String fromObject(StickerSelection in) {
    return GsonUtil.getGsonInstance().toJson(in);
  }

  @TypeConverter
  public StickerSelection toStickerSelection(String in) {
    return GsonUtil.getGsonInstance().fromJson(in, StickerSelection.class);
  }

  @TypeConverter
  @Nullable
  public String fromLocalDate(@Nullable LocalDate in) {
    return DateUtil.toWireStr(in);
  }
  @TypeConverter
  public LocalDate toLocalDate(String in) {
    return DateUtil.fromWireStr(in);
  }

  @TypeConverter
  public Duration toDuration(String in) {
    return in == null ? null : new Duration(Long.valueOf(in));
  }

  @TypeConverter
  @Nullable
  public String fromDuration(@Nullable Duration d) {
    return d == null ? null : String.valueOf(d.getMillis());
  }

  @TypeConverter
  public IntercourseTimeOfDay toTimeOfDay(String in) {
    return IntercourseTimeOfDay.fromStr(in);
  }
  @TypeConverter
  public String fromTimeOfDay(IntercourseTimeOfDay in) {
    return in.name();
  }

  @TypeConverter
  public String fromMap(BoolMapping in) {
    return GsonUtil.getGsonInstance().toJson(in);
  }

  @TypeConverter
  public BoolMapping toMap(String in) {
    return GsonUtil.getGsonInstance().fromJson(in, BoolMapping.class);
  }

  @TypeConverter
  public List<BasicInstruction> toInstructionList(String wire) {
    Type type = new TypeToken<List<BasicInstruction>>() {}.getType();
    return GsonUtil.getGsonInstance().fromJson(wire, type);
  }

  @TypeConverter
  public String fromInstructionList(List<BasicInstruction> basicInstructionList) {
    Type type = new TypeToken<List<BasicInstruction>>() {}.getType();
    return GsonUtil.getGsonInstance().toJson(basicInstructionList, type);
  }

  @TypeConverter
  public List<SpecialInstruction> toSpecialInstructionList(String wire) {
    Type type = new TypeToken<List<SpecialInstruction>>() {}.getType();
    return GsonUtil.getGsonInstance().fromJson(wire, type);
  }

  @TypeConverter
  public String fromSpecialInstructionList(List<SpecialInstruction> in) {
    Type type = new TypeToken<List<BasicInstruction>>() {}.getType();
    return GsonUtil.getGsonInstance().toJson(in, type);
  }

  @TypeConverter
  public String fromYellowStampInstructionList(List<YellowStampInstruction> in) {
    Type type = new TypeToken<List<YellowStampInstruction>>() {}.getType();
    return GsonUtil.getGsonInstance().toJson(in, type);
  }

  @TypeConverter
  public List<YellowStampInstruction> toYellowStampInstructionList(String wire) {
    Type type = new TypeToken<List<YellowStampInstruction>>() {}.getType();
    return GsonUtil.getGsonInstance().fromJson(wire, type);
  }

  @TypeConverter
  public String fromMonitorReading(MonitorReading in) {
    return in.name();
  }

  @TypeConverter
  public MonitorReading toMonitorReading(String in) {
    return MonitorReading.valueOf(in);
  }

  @TypeConverter
  public String fromTimeOfDay(Prescription.TimeOfDay in) {
    return in.name();
  }

  @TypeConverter
  public Prescription.TimeOfDay toMedicationTimeOfDay(String in) {
    return Prescription.TimeOfDay.valueOf(in);
  }

  @TypeConverter
  public String fromLHTestResult(LHTestResult in) {
    return in.name();
  }

  @TypeConverter
  public LHTestResult toLHTestResult(String in) {
    return LHTestResult.valueOf(in);
  }

  @TypeConverter
  public DateTime toDateTime(long in) {
    return new DateTime(in);
  }

  @TypeConverter
  public long fromDateTime(DateTime in) {
    return in.getMillis();
  }
}

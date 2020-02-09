package com.bloomcyclecare.cmcc.data.db;

import com.bloomcyclecare.cmcc.crypto.AesCryptoUtil;
import com.bloomcyclecare.cmcc.data.domain.BasicInstruction;
import com.bloomcyclecare.cmcc.data.domain.IntercourseTimeOfDay;
import com.bloomcyclecare.cmcc.data.domain.Observation;
import com.bloomcyclecare.cmcc.data.domain.SpecialInstruction;
import com.bloomcyclecare.cmcc.data.domain.YellowStampInstruction;
import com.bloomcyclecare.cmcc.utils.BoolMapping;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.bloomcyclecare.cmcc.utils.GsonUtil;
import com.google.gson.reflect.TypeToken;

import org.joda.time.LocalDate;

import java.lang.reflect.Type;
import java.util.List;

import javax.crypto.SecretKey;

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
  @Nullable
  public String fromLocalDate(@Nullable LocalDate in) {
    return DateUtil.toWireStr(in);
  }
  @TypeConverter
  public LocalDate toLocalDate(String in) {
    return DateUtil.fromWireStr(in);
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
  public String fromSecretKey(SecretKey key) {
    return AesCryptoUtil.serializeKey(key);
  }

  @TypeConverter
  public SecretKey fromString(String in) {
    return AesCryptoUtil.parseKey(in);
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
}

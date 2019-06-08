package com.roamingroths.cmcc.data.db;

import androidx.annotation.Nullable;
import androidx.room.TypeConverter;

import com.google.gson.reflect.TypeToken;
import com.roamingroths.cmcc.crypto.AesCryptoUtil;
import com.roamingroths.cmcc.data.domain.Instruction;
import com.roamingroths.cmcc.data.domain.IntercourseTimeOfDay;
import com.roamingroths.cmcc.data.domain.Observation;
import com.roamingroths.cmcc.data.domain.SpecialInstruction;
import com.roamingroths.cmcc.utils.BoolMapping;
import com.roamingroths.cmcc.utils.DateUtil;
import com.roamingroths.cmcc.utils.GsonUtil;

import org.joda.time.LocalDate;

import java.lang.reflect.Type;
import java.util.List;

import javax.crypto.SecretKey;

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
  public List<Instruction> toInstructionList(String wire) {
    Type type = new TypeToken<List<Instruction>>() {}.getType();
    return GsonUtil.getGsonInstance().fromJson(wire, type);
  }

  @TypeConverter
  public String fromInstructionList(List<Instruction> instructionList) {
    Type type = new TypeToken<List<Instruction>>() {}.getType();
    return GsonUtil.getGsonInstance().toJson(instructionList, type);
  }

  @TypeConverter
  public List<SpecialInstruction> toSpecialInstructionList(String wire) {
    Type type = new TypeToken<List<SpecialInstruction>>() {}.getType();
    return GsonUtil.getGsonInstance().fromJson(wire, type);
  }

  @TypeConverter
  public String fromSpecialInstructionList(List<SpecialInstruction> in) {
    Type type = new TypeToken<List<Instruction>>() {}.getType();
    return GsonUtil.getGsonInstance().toJson(in, type);
  }
}

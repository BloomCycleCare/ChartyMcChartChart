package com.roamingroths.cmcc.logic.chart;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.common.base.Objects;
import com.roamingroths.cmcc.crypto.AesCryptoUtil;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 4/22/17.
 */

public class ObservationEntry extends Entry implements Parcelable {

  @Nullable public Observation observation;
  public boolean peakDay;
  public boolean intercourse;
  public boolean firstDay;
  public boolean pointOfChange;
  public boolean unusualBleeding;
  public IntercourseTimeOfDay intercourseTimeOfDay;
  public boolean isEssentiallyTheSame;

  public ObservationEntry(
      LocalDate date,
      @Nullable Observation observation,
      boolean peakDay,
      boolean intercourse,
      boolean firstDay,
      boolean pointOfChange,
      boolean unusualBleeding,
      IntercourseTimeOfDay intercourseTimeOfDay,
      boolean isEssentiallyTheSame,
      SecretKey key) {
    super(date);
    this.observation = observation;
    this.peakDay = peakDay;
    this.intercourse = intercourse;
    this.firstDay = firstDay;
    this.pointOfChange = pointOfChange;
    if (unusualBleeding && (observation == null || !observation.hasBlood())) {
      throw new IllegalArgumentException();
    }
    this.unusualBleeding = unusualBleeding;
    this.intercourseTimeOfDay = intercourseTimeOfDay;
    this.isEssentiallyTheSame = isEssentiallyTheSame;
    swapKey(key);
  }

  public ObservationEntry(Parcel in) {
    this(
        DateUtil.fromWireStr(in.readString()),
        in.<Observation>readParcelable(Observation.class.getClassLoader()),
        in.readByte() != 0,
        in.readByte() != 0,
        in.readByte() != 0,
        in.readByte() != 0,
        in.readByte() != 0,
        IntercourseTimeOfDay.valueOf(in.readString()),
        in.readByte() != 0,
        AesCryptoUtil.parseKey(in.readString()));
  }

  @Override
  public boolean maybeUpgrade() {
    if (intercourseTimeOfDay == null) {
      intercourseTimeOfDay = intercourse ? IntercourseTimeOfDay.ANY : IntercourseTimeOfDay.NONE;
      return true;
    }
    return false;
  }

  public static ObservationEntry emptyEntry(LocalDate date, SecretKey secretKey) {
    return new ObservationEntry(date, null, false, false, false, false, false, IntercourseTimeOfDay.NONE, false, secretKey);
  }

  @Override
  public List<String> getSummaryLines() {
    List<String> lines = new ArrayList<>();
    if (observation != null) {
      lines.addAll(observation.getSummaryLines());
    } else {
      lines.add("Empty Observation");
    }
    return lines;
  }

  public static final Creator<ObservationEntry> CREATOR = new Creator<ObservationEntry>() {
    @Override
    public ObservationEntry createFromParcel(Parcel in) {
      return new ObservationEntry(in);
    }

    @Override
    public ObservationEntry[] newArray(int size) {
      return new ObservationEntry[size];
    }
  };

  public boolean hasMucus() {
    if (observation == null || observation.dischargeSummary == null) {
      return false;
    }
    return observation.dischargeSummary.hasMucus();
  }

  public boolean hasPeakTypeMucus() {
    return hasMucus() && observation.dischargeSummary.isPeakType();
  }

  public String getListUiText() {
    if (observation == null) {
      return "----";
    }
    return String.format("%s %s", observation.toString(), intercourse ? "I" : "");
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(getDateStr());
    dest.writeParcelable(observation, flags);
    dest.writeByte((byte) (peakDay ? 1 : 0));
    dest.writeByte((byte) (intercourse ? 1 : 0));
    dest.writeByte((byte) (firstDay ? 1 : 0));
    dest.writeByte((byte) (pointOfChange ? 1 : 0));
    dest.writeByte((byte) (unusualBleeding ? 1 : 0));
    dest.writeString(intercourseTimeOfDay.name());
    dest.writeByte((byte) (isEssentiallyTheSame ? 1 : 0));
    dest.writeString(AesCryptoUtil.serializeKey(getKey()));
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ObservationEntry) {
      ObservationEntry that = (ObservationEntry) o;
      return Objects.equal(this.observation, that.observation) &&
          Objects.equal(this.getDate(), that.getDate()) &&
          this.peakDay == that.peakDay &&
          this.intercourse == that.intercourse &&
          this.firstDay == that.firstDay &&
          this.pointOfChange == that.pointOfChange &&
          this.unusualBleeding == that.unusualBleeding &&
          this.isEssentiallyTheSame == that.isEssentiallyTheSame &&
          this.intercourseTimeOfDay == that.intercourseTimeOfDay;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        observation, peakDay, intercourse, getDate(), firstDay, pointOfChange, unusualBleeding, intercourseTimeOfDay, isEssentiallyTheSame);
  }

  public enum IntercourseTimeOfDay {
    NONE("None"), ANY("Any time of day"), END("End of day");

    final String value;

    IntercourseTimeOfDay(String value) {
      this.value = value;
    }

    public static IntercourseTimeOfDay fromStr(String str) {
      for (IntercourseTimeOfDay item : IntercourseTimeOfDay.values()) {
        if (item.value.equals(str)) {
          return item;
        }
      }
      throw new IllegalArgumentException(str + ": does not match any values");
    }
  }
}

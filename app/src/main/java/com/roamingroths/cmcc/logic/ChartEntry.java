package com.roamingroths.cmcc.logic;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.common.base.Objects;
import com.google.firebase.database.DataSnapshot;
import com.roamingroths.cmcc.crypto.AesCryptoUtil;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 4/22/17.
 */

public class ChartEntry extends Entry implements Parcelable {

  @Nullable public Observation observation;
  public boolean peakDay;
  public boolean intercourse;
  public boolean firstDay;
  public boolean pointOfChange;
  public boolean unusualBleeding;

  public ChartEntry(
      LocalDate date,
      @Nullable Observation observation,
      boolean peakDay,
      boolean intercourse,
      boolean firstDay,
      boolean pointOfChange,
      boolean unusualBleeding,
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
    swapKey(key);
  }

  public ChartEntry(Parcel in) {
    this(
        DateUtil.fromWireStr(in.readString()),
        in.<Observation>readParcelable(Observation.class.getClassLoader()),
        in.readByte() != 0,
        in.readByte() != 0,
        in.readByte() != 0,
        in.readByte() != 0,
        in.readByte() != 0,
        AesCryptoUtil.parseKey(in.readString()));
  }

  public static ChartEntry emptyEntry(LocalDate date, SecretKey secretKey) {
    return new ChartEntry(date, null, false, false, false, false, false, secretKey);
  }

  public static void fromEncryptedString(
      String encryptedEntry, SecretKey secretKey, Callbacks.Callback<ChartEntry> callback) {
    CryptoUtil.decrypt(encryptedEntry, secretKey, ChartEntry.class, callback);
  }

  public static void fromSnapshot(
      DataSnapshot snapshot, SecretKey secretKey, Callbacks.Callback<ChartEntry> callback) {
    fromEncryptedString(snapshot.getValue(String.class), secretKey, callback);
  }

  public static final Creator<ChartEntry> CREATOR = new Creator<ChartEntry>() {
    @Override
    public ChartEntry createFromParcel(Parcel in) {
      return new ChartEntry(in);
    }

    @Override
    public ChartEntry[] newArray(int size) {
      return new ChartEntry[size];
    }
  };

  public boolean hasMucus() {
    if (observation == null || observation.dischargeSummary == null) {
      return false;
    }
    return observation.dischargeSummary.hasMucus();
  }

  public String getListUiText() {
    if (observation == null) {
      return "----";
    }
    return observation.toString();
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
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ChartEntry) {
      ChartEntry that = (ChartEntry) o;
      return Objects.equal(this.observation, that.observation) &&
          Objects.equal(this.getDate(), that.getDate()) &&
          this.peakDay == that.peakDay &&
          this.intercourse == that.intercourse &&
          this.firstDay == that.firstDay &&
          this.pointOfChange == that.pointOfChange &&
          this.unusualBleeding == that.unusualBleeding;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        observation, peakDay, intercourse, getDate(), firstDay, pointOfChange, unusualBleeding);
  }
}

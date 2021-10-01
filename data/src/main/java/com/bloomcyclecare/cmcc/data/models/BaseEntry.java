package com.bloomcyclecare.cmcc.data.models;

import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

import com.bloomcyclecare.cmcc.utils.DateUtil;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.List;
import java.util.Objects;

/**
 * Created by parkeroth on 9/20/17.
 */
public abstract class BaseEntry implements Entry {

  @PrimaryKey
  @NonNull
  @ColumnInfo(name = "entryDate")
  public LocalDate mEntryDate;

  @ColumnInfo(name = "timeCreated")
  public DateTime mTimeCreated;

  @ColumnInfo(name = "timeUpdated")
  public DateTime mTimeUpdated;

  @ColumnInfo(name = "timesUpdated")
  public int mTimesUpdated;

  public BaseEntry() {}

  private BaseEntry(
      @NonNull LocalDate entryDate,
      @Nullable DateTime timeCreated,
      @Nullable DateTime timeUpdated,
      int timesUpdated) {
    mEntryDate = entryDate;
    mTimeCreated = timeCreated;
    mTimeUpdated = timeUpdated;
    mTimesUpdated = timesUpdated;
  }

  protected BaseEntry(@NonNull LocalDate entryDate) {
    this(entryDate, null, null, 0);
  }

  protected BaseEntry(Parcel in) {
    this(
        DateUtil.fromWireStr(in.readString()),
        DateUtil.fromWire(in.readLong()),
        DateUtil.fromWire(in.readLong()),
        in.readInt());
  }

  protected BaseEntry(BaseEntry entry) {
    this(entry.getDate(), entry.mTimeCreated, entry.mTimeUpdated, entry.mTimesUpdated);
  }

  protected void fillParcel(Parcel dest) {
    dest.writeString(getDateStr());
    dest.writeLong(mTimeCreated == null ? -1 : mTimeCreated.getMillis());
    dest.writeLong(mTimeUpdated == null ? -1 : mTimeUpdated.getMillis());
    dest.writeInt(mTimesUpdated);
  }

  public LocalDate getDate() {
    return mEntryDate;
  }

  public String getDateStr() {
    return DateUtil.toWireStr(mEntryDate);
  }

  public abstract List<String> getSummaryLines();

  @Override
  public void setTimeUpdated(DateTime time) {
    mTimeUpdated = time;
    mTimesUpdated++;
  }

  @Override
  public DateTime timeUpdated() {
    return mTimeUpdated;
  }

  @Override
  public void setTimeCreated(DateTime time) {
    mTimeCreated = time;
    mTimesUpdated++;
  }

  @Override
  public DateTime timeCreated() {
    return mTimeCreated;
  }

  @Override
  public int timesUpdated() {
    return mTimesUpdated;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BaseEntry entry = (BaseEntry) o;
    return mEntryDate.equals(entry.mEntryDate) &&
        mTimesUpdated == entry.mTimesUpdated &&
        Objects.equals(mTimeCreated, entry.mTimeCreated) &&
        Objects.equals(mTimeUpdated, entry.mTimeUpdated);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mEntryDate, mTimeCreated, mTimeUpdated, mTimesUpdated);
  }
}

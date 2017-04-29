package com.roamingroths.cmcc.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Created by parkeroth on 4/22/17.
 */

public class ChartEntry implements Parcelable {

  private static final Joiner ON_COMMA = Joiner.on(',');

  public final Observation observation;
  public final boolean peakDay;
  public final boolean intercourse;

  public ChartEntry(Observation observation, boolean peakDay, boolean intercourse) {
    this.observation = Preconditions.checkNotNull(observation);
    this.peakDay = peakDay;
    this.intercourse = intercourse;
  }

  public ChartEntry(Parcel in) {
    observation = in.readParcelable(Observation.class.getClassLoader());
    peakDay = in.readByte() != 0;
    intercourse = in.readByte() != 0;
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

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(observation, flags);
    dest.writeByte((byte) (peakDay ? 1 : 0));
    dest.writeByte((byte) (intercourse ? 1 : 0));
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ChartEntry) {
      ChartEntry that = (ChartEntry) o;
      return this.observation.equals(that.observation) &&
          this.peakDay == that.peakDay &&
          this.intercourse == that.intercourse;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(observation, peakDay, intercourse);
  }
}

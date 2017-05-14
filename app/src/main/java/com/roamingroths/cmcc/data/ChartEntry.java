package com.roamingroths.cmcc.data;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.firebase.database.DataSnapshot;
import com.roamingroths.cmcc.utils.CryptoUtil;
import com.roamingroths.cmcc.utils.DateUtil;

import java.security.PrivateKey;
import java.text.ParseException;
import java.util.Date;

/**
 * Created by parkeroth on 4/22/17.
 */

public class ChartEntry implements Parcelable {

  public Date date;
  public Observation observation;
  public boolean peakDay;
  public boolean intercourse;

  public ChartEntry() {
    // Required for DataSnapshot.getValue(ChartEntry.class)
  }

  public ChartEntry(Date date, Observation observation, boolean peakDay, boolean intercourse) {
    this.date = date;
    this.observation = Preconditions.checkNotNull(observation);
    this.peakDay = peakDay;
    this.intercourse = intercourse;
  }

  public ChartEntry(Parcel in) {
    try {
      date = parseDate(in.readString());
    } catch (ParseException pe) {
      // TODO: Handle ParseException better
      throw new IllegalStateException(pe);
    }
    observation = in.readParcelable(Observation.class.getClassLoader());
    peakDay = in.readByte() != 0;
    intercourse = in.readByte() != 0;
  }

  public static ChartEntry fromSnapshot(DataSnapshot snapshot, Context context)
      throws CryptoUtil.CryptoException {
    String entryDateStr = snapshot.getKey();
    String encryptedEntry = snapshot.getValue(String.class);
    PrivateKey privateKey = CryptoUtil.getPersonalPrivateKey(context);
    ChartEntry entry = CryptoUtil.decrypt(encryptedEntry, privateKey, ChartEntry.class);
    Preconditions.checkArgument(entry.getDateStr().equals(entryDateStr));
    return entry;
  }

  public static final Date parseDate(String dateStr) throws ParseException {
    return DateUtil.fromWireStr(dateStr);
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

  public String getDateStr() {
    return DateUtil.toWireStr(date);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(DateUtil.toWireStr(date));
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
          this.intercourse == that.intercourse &&
          this.date == that.date;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(observation, peakDay, intercourse, date);
  }
}

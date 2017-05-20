package com.roamingroths.cmcc.data;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.firebase.database.DataSnapshot;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.utils.CryptoUtil;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import java.security.PrivateKey;

/**
 * Created by parkeroth on 4/22/17.
 */

public class ChartEntry implements Parcelable {

  public LocalDate date;
  public Observation observation;
  public boolean peakDay;
  public boolean intercourse;
  public boolean firstDay;

  public ChartEntry() {
    // Required for DataSnapshot.getValue(ChartEntry.class)
  }

  public ChartEntry(LocalDate date, Observation observation, boolean peakDay, boolean intercourse, boolean firstDay) {
    this.date = date;
    this.observation = Preconditions.checkNotNull(observation);
    this.peakDay = peakDay;
    this.intercourse = intercourse;
    this.firstDay = firstDay;
  }

  public ChartEntry(Parcel in) {
    this(
        DateUtil.fromWireStr(in.readString()),
        in.<Observation>readParcelable(Observation.class.getClassLoader()),
        in.readByte() != 0,
        in.readByte() != 0,
        in.readByte() != 0);
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
    dest.writeByte((byte) (firstDay ? 1 : 0));
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ChartEntry) {
      ChartEntry that = (ChartEntry) o;
      return this.observation.equals(that.observation) &&
          this.peakDay == that.peakDay &&
          this.intercourse == that.intercourse &&
          this.date.equals(that.date) &&
          this.firstDay == that.firstDay;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(observation, peakDay, intercourse, date, firstDay);
  }

  public int getEntryColorResource() {
    if (observation.flow != null) {
      return R.color.entryRed;
    }
    if (observation.dischargeSummary.mType == DischargeSummary.DischargeType.DRY) {
      return R.color.entryGreen;
    }
    return R.color.entryWhite;
  }
}

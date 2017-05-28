package com.roamingroths.cmcc.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;

import com.google.common.base.Objects;
import com.google.firebase.database.DataSnapshot;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.CryptoUtil;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

/**
 * Created by parkeroth on 4/22/17.
 */

public class ChartEntry implements Parcelable {

  public LocalDate date;
  @Nullable public Observation observation;
  public boolean peakDay;
  public boolean intercourse;
  public boolean firstDay;

  public ChartEntry() {
    // Required for DataSnapshot.getValue(ChartEntry.class)
  }

  public ChartEntry(LocalDate date, @Nullable Observation observation, boolean peakDay, boolean intercourse, boolean firstDay) {
    this.date = date;
    this.observation = observation;
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

  public static ChartEntry emptyEntry(LocalDate date) {
    return new ChartEntry(date, null, false, false, false);
  }

  public static void fromEncryptedString(
      String encryptedEntry, Context context, Callbacks.Callback<ChartEntry> callback) {
    CryptoUtil.decrypt(encryptedEntry, context, ChartEntry.class, callback);
  }

  public static void fromSnapshot(
      DataSnapshot snapshot, Context context, Callbacks.Callback<ChartEntry> callback) {
    fromEncryptedString(snapshot.getValue(String.class), context, callback);
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

  public String getListUiText() {
    if (observation == null) {
      return "----";
    }
    return observation.toString();
  }

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
      return Objects.equal(this.observation, that.observation) &&
          Objects.equal(this.date, that.date) &&
          this.peakDay == that.peakDay &&
          this.intercourse == that.intercourse &&
          this.firstDay == that.firstDay;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(observation, peakDay, intercourse, date, firstDay);
  }

  public int getEntryColorResource(Context context) {
    if (observation == null) {
      return R.color.entryGrey;
    }
    if (observation.flow != null) {
      return R.color.entryRed;
    }
    if (observation.dischargeSummary.mType == DischargeSummary.DischargeType.DRY) {
      return R.color.entryGreen;
    }
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    boolean useYellowPrepeak = preferences.getBoolean("enable_pre_peak_yellow_stickers", false);
    boolean useYellowPostPeak = preferences.getBoolean("enable_post_peak_yellow_stickers", false);
    // TODO: real implementation
    if (useYellowPostPeak) {
      return R.color.entryYellow;
    }
    return R.color.entryWhite;
  }
}

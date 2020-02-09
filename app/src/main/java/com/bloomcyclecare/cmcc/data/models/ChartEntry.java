package com.bloomcyclecare.cmcc.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.bloomcyclecare.cmcc.data.entities.ObservationEntry;
import com.bloomcyclecare.cmcc.data.entities.SymptomEntry;
import com.bloomcyclecare.cmcc.data.entities.WellnessEntry;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import org.joda.time.LocalDate;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;


/**
 * Created by parkeroth on 9/23/17.
 */

public class ChartEntry implements Parcelable {
  public final LocalDate entryDate;
  public final ObservationEntry observationEntry;
  public final WellnessEntry wellnessEntry;
  public final SymptomEntry symptomEntry;

  public ChartEntry(LocalDate entryDate, ObservationEntry observationEntry, WellnessEntry wellnessEntry, SymptomEntry symptomEntry) {
    this.entryDate = entryDate;
    Preconditions.checkArgument(
        observationEntry.getDate().equals(entryDate), entryDate + " != " + observationEntry.getDate());
    this.observationEntry = observationEntry;
    this.wellnessEntry = wellnessEntry;
    this.symptomEntry = symptomEntry;
  }

  protected ChartEntry(Parcel in) {
    this(DateUtil.fromWireStr(in.readString()),
        Parcels.unwrap(in.readParcelable(ObservationEntry.class.getClassLoader())),
        in.readParcelable(WellnessEntry.class.getClassLoader()),
        in.readParcelable(SymptomEntry.class.getClassLoader()));
  }

  public static ChartEntry emptyEntry(LocalDate entryDate) {
    return new ChartEntry(
        LocalDate.now(), ObservationEntry.emptyEntry(entryDate), WellnessEntry.emptyEntry(entryDate), SymptomEntry.emptyEntry(entryDate));
  }

  @NonNull
  @Override
  public String toString() {
    return entryDate.toString();
  }

  public List<String> getSummaryLines() {
    List<String> lines = new ArrayList<>();
    List<String> chartEntryLines = observationEntry.getSummaryLines();
    if (!chartEntryLines.isEmpty()) {
      lines.addAll(chartEntryLines);
    }
    List<String> wellnessLines = wellnessEntry.getSummaryLines();
    if (!wellnessLines.isEmpty()) {
      lines.add(" ");
      lines.addAll(wellnessLines);
    }
    List<String> symptomLines = symptomEntry.getSummaryLines();
    if (!symptomLines.isEmpty()) {
      lines.add(" ");
      lines.addAll(symptomLines);
    }
    return lines;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(DateUtil.toWireStr(entryDate));
    dest.writeParcelable(Parcels.wrap(observationEntry), flags);
    dest.writeParcelable(wellnessEntry, flags);
    dest.writeParcelable(symptomEntry, flags);
  }

  @Override
  public int describeContents() {
    return 0;
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
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj instanceof ChartEntry) {
      ChartEntry that = (ChartEntry) obj;

      return Objects.equal(this.entryDate, that.entryDate)
          && Objects.equal(this.observationEntry, that.observationEntry)
          && Objects.equal(this.wellnessEntry, that.wellnessEntry)
          && Objects.equal(this.symptomEntry, that.symptomEntry);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(entryDate, observationEntry, symptomEntry, wellnessEntry);
  }
}

package com.roamingroths.cmcc.logic.chart;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;


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
    entryDate = DateUtil.fromWireStr(in.readString());
    observationEntry = in.readParcelable(ObservationEntry.class.getClassLoader());
    wellnessEntry = in.readParcelable(WellnessEntry.class.getClassLoader());
    symptomEntry = in.readParcelable(SymptomEntry.class.getClassLoader());
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
    dest.writeParcelable(observationEntry, flags);
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

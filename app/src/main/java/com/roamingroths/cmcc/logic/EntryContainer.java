package com.roamingroths.cmcc.logic;

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

public class EntryContainer implements Parcelable {
  public final LocalDate entryDate;
  public final ChartEntry chartEntry;
  public final WellnessEntry wellnessEntry;
  public final SymptomEntry symptomEntry;

  public EntryContainer(LocalDate entryDate, ChartEntry chartEntry, WellnessEntry wellnessEntry, SymptomEntry symptomEntry) {
    this.entryDate = entryDate;
    Preconditions.checkArgument(
        chartEntry.getDate().equals(entryDate), entryDate + " != " + chartEntry.getDate());
    this.chartEntry = chartEntry;
    this.wellnessEntry = wellnessEntry;
    this.symptomEntry = symptomEntry;
  }

  protected EntryContainer(Parcel in) {
    entryDate = DateUtil.fromWireStr(in.readString());
    chartEntry = in.readParcelable(ChartEntry.class.getClassLoader());
    wellnessEntry = in.readParcelable(WellnessEntry.class.getClassLoader());
    symptomEntry = in.readParcelable(SymptomEntry.class.getClassLoader());
  }

  public List<String> getSummaryLines() {
    List<String> lines = new ArrayList<>();
    List<String> chartEntryLines = chartEntry.getSummaryLines();
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
    dest.writeParcelable(chartEntry, flags);
    dest.writeParcelable(wellnessEntry, flags);
    dest.writeParcelable(symptomEntry, flags);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<EntryContainer> CREATOR = new Creator<EntryContainer>() {
    @Override
    public EntryContainer createFromParcel(Parcel in) {
      return new EntryContainer(in);
    }

    @Override
    public EntryContainer[] newArray(int size) {
      return new EntryContainer[size];
    }
  };

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof EntryContainer) {
      EntryContainer that = (EntryContainer) obj;
      return this.entryDate.equals(that.entryDate)
          && this.chartEntry.equals(that.chartEntry)
          && this.symptomEntry.equals(that.symptomEntry)
          && this.wellnessEntry.equals(that.wellnessEntry);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(entryDate, chartEntry, symptomEntry, wellnessEntry);
  }
}

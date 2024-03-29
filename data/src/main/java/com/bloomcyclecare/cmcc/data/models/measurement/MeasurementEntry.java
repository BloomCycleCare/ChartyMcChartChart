package com.bloomcyclecare.cmcc.data.models.measurement;

import androidx.room.Entity;
import androidx.room.Ignore;

import com.bloomcyclecare.cmcc.data.models.Entry;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;
import org.parceler.Parcel;

import java.util.List;
import java.util.Objects;

@Entity
@Parcel
public class MeasurementEntry extends Entry {

  public MonitorReading monitorReading;
  public LHTestResult lhTestResult;

  @Ignore
  public MeasurementEntry(
      Entry entry, MonitorReading monitorReading, LHTestResult lhTestResult) {
    super(entry);

    this.monitorReading = monitorReading;
    this.lhTestResult = lhTestResult;
  }

  private MeasurementEntry(LocalDate date) {
    super(date);
    this.monitorReading = MonitorReading.UNKNOWN;
    this.lhTestResult = LHTestResult.NONE;
  }

  public MeasurementEntry() {
    super();
  }

  @Override
  public List<String> getSummaryLines() {
    ImmutableList.Builder<String> lines = ImmutableList.builder();
    if (monitorReading != MonitorReading.UNKNOWN) {
      lines.add(String.format("Monitor Reading: %s", monitorReading.name()));
    }
    if (lhTestResult != LHTestResult.NONE) {
      lines.add(String.format("LH Test Result: %s", lhTestResult.name()));
    }
    List<String> summary = lines.build();
    return !summary.isEmpty() ? summary : ImmutableList.of("No measurements");
  }

  public static MeasurementEntry emptyEntry(LocalDate entryDate) {
    return new MeasurementEntry(entryDate);
  }

  public boolean isEmpty() {
    return monitorReading == MonitorReading.UNKNOWN && lhTestResult == LHTestResult.NONE;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MeasurementEntry that = (MeasurementEntry) o;
    return super.equals(that)
        && Objects.equals(monitorReading, that.monitorReading)
        && Objects.equals(lhTestResult, that.lhTestResult);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mEntryDate, monitorReading, lhTestResult);
  }
}

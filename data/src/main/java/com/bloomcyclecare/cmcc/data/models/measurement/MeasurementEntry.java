package com.bloomcyclecare.cmcc.data.models.measurement;

import com.bloomcyclecare.cmcc.data.models.Entry;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;
import org.parceler.Parcel;

import java.util.List;
import java.util.Objects;

import androidx.room.Entity;
import androidx.room.Ignore;

@Entity
@Parcel
public class MeasurementEntry extends Entry {

  public MonitorReading monitorReading;
  public LHTestResult lhTestResult;

  @Ignore
  public MeasurementEntry(
      LocalDate entryDate, MonitorReading monitorReading, LHTestResult lhTestResult) {
    super(entryDate);

    this.monitorReading = monitorReading;
    this.lhTestResult = lhTestResult;
  }

  public MeasurementEntry() {
    super();
  }

  @Override
  public List<String> getSummaryLines() {
    return ImmutableList.of("TODO");
  }

  public static MeasurementEntry emptyEntry(LocalDate entryDate) {
    return new MeasurementEntry(entryDate, MonitorReading.UNKNOWN, LHTestResult.NONE);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MeasurementEntry that = (MeasurementEntry) o;
    return mEntryDate == that.mEntryDate && monitorReading == that.monitorReading &&
        lhTestResult == that.lhTestResult;
  }

  @Override
  public int hashCode() {
    return Objects.hash(mEntryDate, monitorReading, lhTestResult);
  }
}

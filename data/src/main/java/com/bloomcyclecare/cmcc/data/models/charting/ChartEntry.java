package com.bloomcyclecare.cmcc.data.models.charting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bloomcyclecare.cmcc.data.models.Entry;
import com.bloomcyclecare.cmcc.data.models.breastfeeding.BreastfeedingEntry;
import com.bloomcyclecare.cmcc.data.models.wellbeing.WellbeingEntry;
import com.bloomcyclecare.cmcc.data.models.measurement.MeasurementEntry;
import com.bloomcyclecare.cmcc.data.models.observation.ObservationEntry;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelection;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;
import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by parkeroth on 9/23/17.
 */
@Parcel
public class ChartEntry implements Comparable<ChartEntry> {
  public LocalDate entryDate;
  public ObservationEntry observationEntry;
  public MeasurementEntry measurementEntry;
  public BreastfeedingEntry breastfeedingEntry;
  public WellbeingEntry wellbeingEntry;
  @Nullable public StickerSelection stickerSelection;

  public String marker = "";

  public ChartEntry() {}

  public ChartEntry(LocalDate entryDate,
                    ObservationEntry observationEntry,
                    MeasurementEntry measurementEntry,
                    BreastfeedingEntry breastfeedingEntry,
                    WellbeingEntry wellbeingEntry,
                    @Nullable StickerSelection stickerSelection) {
    this.entryDate = entryDate;
    Preconditions.checkArgument(
        observationEntry.getDate().equals(entryDate), entryDate + " != " + observationEntry.getDate());
    this.observationEntry = observationEntry;
    this.measurementEntry = measurementEntry;
    this.breastfeedingEntry = breastfeedingEntry;
    this.wellbeingEntry = wellbeingEntry;
    this.stickerSelection = stickerSelection;
  }

  public static ChartEntry emptyEntry(LocalDate entryDate) {
    return new ChartEntry(
        entryDate,
        ObservationEntry.emptyEntry(entryDate),
        MeasurementEntry.emptyEntry(entryDate),
        BreastfeedingEntry.emptyEntry(entryDate),
        WellbeingEntry.emptyEntry(entryDate),
        null);
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
    List<Entry> entries = ImmutableList.of(measurementEntry, breastfeedingEntry, wellbeingEntry);
    for (Entry entry : entries) {
      List<String> entryLines = entry.getSummaryLines();
      if (!entryLines.isEmpty()) {
        lines.add(" ");
        lines.addAll(entryLines);
      }
    }
    return lines;
  }

  public boolean hasObservation() {
    return observationEntry.observation != null;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj instanceof ChartEntry) {
      ChartEntry that = (ChartEntry) obj;

      return Objects.equal(this.entryDate, that.entryDate)
          && Objects.equal(this.observationEntry, that.observationEntry)
          && Objects.equal(this.measurementEntry, that.measurementEntry)
          && Objects.equal(this.breastfeedingEntry, that.breastfeedingEntry)
          && Objects.equal(this.wellbeingEntry, that.wellbeingEntry)
          && Objects.equal(this.stickerSelection, that.stickerSelection);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(entryDate, observationEntry, measurementEntry, breastfeedingEntry, wellbeingEntry, stickerSelection);
  }

  @Override
  public int compareTo(ChartEntry o) {
    return this.entryDate.compareTo(o.entryDate);
  }
}

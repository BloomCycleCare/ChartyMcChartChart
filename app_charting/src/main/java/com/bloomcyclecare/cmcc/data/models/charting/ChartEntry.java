package com.bloomcyclecare.cmcc.data.models.charting;

import com.bloomcyclecare.cmcc.data.models.observation.ObservationEntry;
import com.bloomcyclecare.cmcc.data.models.observation.SymptomEntry;
import com.bloomcyclecare.cmcc.data.models.observation.WellnessEntry;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelection;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import org.joda.time.LocalDate;
import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 * Created by parkeroth on 9/23/17.
 */
@Parcel
public class ChartEntry implements Comparable<ChartEntry> {
  public LocalDate entryDate;
  public ObservationEntry observationEntry;
  public WellnessEntry wellnessEntry;
  public SymptomEntry symptomEntry;
  @Nullable public StickerSelection stickerSelection;

  public String marker = "";

  public ChartEntry() {}

  public ChartEntry(LocalDate entryDate, ObservationEntry observationEntry, WellnessEntry wellnessEntry, SymptomEntry symptomEntry, @Nullable StickerSelection stickerSelection) {
    this.entryDate = entryDate;
    Preconditions.checkArgument(
        observationEntry.getDate().equals(entryDate), entryDate + " != " + observationEntry.getDate());
    this.observationEntry = observationEntry;
    this.wellnessEntry = wellnessEntry;
    this.symptomEntry = symptomEntry;
    this.stickerSelection = stickerSelection;
  }

  @Deprecated
  public static ChartEntry withoutStickerSelection(LocalDate entryDate, ObservationEntry observationEntry, WellnessEntry wellnessEntry, SymptomEntry symptomEntry) {
    return new ChartEntry(entryDate, observationEntry, wellnessEntry, symptomEntry, null);
  }

  public static ChartEntry emptyEntry(LocalDate entryDate) {
    return new ChartEntry(
        entryDate, ObservationEntry.emptyEntry(entryDate), WellnessEntry.emptyEntry(entryDate), SymptomEntry.emptyEntry(entryDate), null);
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
          && Objects.equal(this.wellnessEntry, that.wellnessEntry)
          && Objects.equal(this.symptomEntry, that.symptomEntry)
          && Objects.equal(this.stickerSelection, that.stickerSelection);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(entryDate, observationEntry, symptomEntry, wellnessEntry, stickerSelection);
  }

  @Override
  public int compareTo(ChartEntry o) {
    return this.entryDate.compareTo(o.entryDate);
  }
}

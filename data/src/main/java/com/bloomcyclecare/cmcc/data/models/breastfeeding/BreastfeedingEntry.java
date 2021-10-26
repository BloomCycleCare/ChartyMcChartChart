package com.bloomcyclecare.cmcc.data.models.breastfeeding;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;

import com.bloomcyclecare.cmcc.data.models.Entry;
import com.google.common.collect.ImmutableList;

import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.parceler.Parcel;

import java.util.List;

@Parcel
@Entity
public class BreastfeedingEntry extends Entry {

  public int numDayFeedings;
  public int numNightFeedings;
  @Nullable
  public Duration maxGapBetweenFeedings;

  public BreastfeedingEntry(@NonNull Entry entry, int numDayFeedings, int numNightFeedings, @Nullable Duration maxGapBetweenFeedings) {
    super(entry);
    this.numDayFeedings = numDayFeedings;
    this.numNightFeedings = numNightFeedings;
    this.maxGapBetweenFeedings = maxGapBetweenFeedings;
  }

  public BreastfeedingEntry() {
    super();
  }

  public static BreastfeedingEntry emptyEntry(LocalDate entryDate) {
    return new BreastfeedingEntry(entryDate);
  }

  public BreastfeedingEntry(LocalDate date) {
    super(date);
    this.numDayFeedings = -1;
    this.numNightFeedings = -1;
    this.maxGapBetweenFeedings = null;
  }

  @Override
  public List<String> getSummaryLines() {
    ImmutableList.Builder<String> lines = ImmutableList.builder();
    if (numDayFeedings >= 0) {
      lines.add("Number of day feedings: " + numDayFeedings);
    }
    if (numNightFeedings >= 0) {
      lines.add("Number of night feedings: " + numNightFeedings);
    }
    if (maxGapBetweenFeedings != null) {
      lines.add("Max hours between feedings: " + maxGapBetweenFeedings.getStandardMinutes() / (float) 60);
    }
    return lines.build();
  }
}

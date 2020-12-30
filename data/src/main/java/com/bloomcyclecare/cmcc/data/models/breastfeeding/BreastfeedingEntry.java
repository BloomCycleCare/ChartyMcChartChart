package com.bloomcyclecare.cmcc.data.models.breastfeeding;

import com.bloomcyclecare.cmcc.data.models.Entry;
import com.google.common.collect.ImmutableList;

import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.parceler.Parcel;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Parcel
@Entity
public class BreastfeedingEntry extends Entry {

  public int numTimesDay;
  public int numTimesNight;
  public Duration maxGapHours;

  public BreastfeedingEntry(@NonNull LocalDate entryDate) {
    super(entryDate);
  }

  public BreastfeedingEntry() {
    super();
  }

  public static BreastfeedingEntry emptyEntry(LocalDate entryDate) {
    return new BreastfeedingEntry(entryDate);
  }

  @Override
  public List<String> getSummaryLines() {
    return ImmutableList.of();
  }
}

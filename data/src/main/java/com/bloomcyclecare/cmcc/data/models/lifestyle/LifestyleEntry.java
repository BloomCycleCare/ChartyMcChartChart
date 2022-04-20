package com.bloomcyclecare.cmcc.data.models.lifestyle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;

import com.bloomcyclecare.cmcc.data.models.Entry;
import com.bloomcyclecare.cmcc.utils.Copyable;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;
import org.parceler.Parcel;

import java.util.List;
import java.util.Objects;

@Entity
@Parcel
public class LifestyleEntry extends Entry implements Copyable<LifestyleEntry> {
  @Nullable public Integer painObservationMorning;
  @Nullable public Integer painObservationAfternoon;
  @Nullable public Integer painObservationEvening;
  @Nullable public Integer painObservationNight;

  public LifestyleEntry() {}

  public LifestyleEntry(LifestyleEntry other) {
    super(other);
    this.painObservationMorning = unBox(other.painObservationMorning);
    this.painObservationAfternoon = unBox(other.painObservationAfternoon);
    this.painObservationEvening = unBox(other.painObservationEvening);
    this.painObservationNight = unBox(other.painObservationNight);
  }

  private static Integer unBox(Integer i) {
    if (i == null) {
      return null;
    }
    int unboxed = i;
    return unboxed;
  }

  public static LifestyleEntry emptyEntry(LocalDate localDate) {
    return new LifestyleEntry(localDate);
  }

  public LifestyleEntry(@NonNull LocalDate entryDate) {
    super(entryDate);
  }

  @Override
  public List<String> getSummaryLines() {
    ImmutableList.Builder<String> lines = new ImmutableList.Builder<>();
    if (painObservationMorning != null) {
      lines.add("Morning pain observation: " + painObservationMorning);
    }
    if (painObservationAfternoon != null) {
      lines.add("Afternoon pain observation: " + painObservationAfternoon);
    }
    if (painObservationEvening != null) {
      lines.add("Evening pain observation: " + painObservationEvening);
    }
    if (painObservationNight != null) {
      lines.add("Night pain observation: " + painObservationNight);
    }
    return lines.build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    LifestyleEntry that = (LifestyleEntry) o;
    return Objects.equals(painObservationMorning, that.painObservationMorning) && Objects.equals(painObservationAfternoon, that.painObservationAfternoon) && Objects.equals(painObservationEvening, that.painObservationEvening) && Objects.equals(painObservationNight, that.painObservationNight);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), painObservationMorning, painObservationAfternoon, painObservationEvening, painObservationNight);
  }

  @Nullable
  public Integer getPainObservation(@NonNull PainObservationTime time) {
    switch (time) {
      case MORNING:
        return painObservationMorning;
      case AFTERNOON:
        return painObservationAfternoon;
      case EVENING:
        return painObservationEvening;
      case NIGHT:
        return painObservationNight;
      default:
        throw new IllegalArgumentException();
    }
  }

  public void updatePainObservation(@NonNull PainObservationTime time, @Nullable Integer observation) {
    switch (time) {
      case MORNING:
        painObservationMorning = observation;
        return;
      case AFTERNOON:
        painObservationAfternoon = observation;
        return;
      case EVENING:
        painObservationEvening = observation;
        return;
      case NIGHT:
        painObservationNight = observation;
        return;
      default:
        throw new IllegalArgumentException();
    }
  }

  @Override
  public LifestyleEntry copy() {
    return new LifestyleEntry(this);
  }

  public enum PainObservationTime {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT
  }
}

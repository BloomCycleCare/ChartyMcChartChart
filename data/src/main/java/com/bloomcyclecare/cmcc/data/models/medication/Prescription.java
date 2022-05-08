package com.bloomcyclecare.cmcc.data.models.medication;

import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;

import org.checkerframework.checker.interning.qual.CompareToMethod;
import org.joda.time.LocalDate;
import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.List;

@AutoValue
@Entity(
    primaryKeys = {"medicationId", "startDate"},
    foreignKeys = {
      @ForeignKey(
          entity = Medication.class,
          parentColumns = "id",
          childColumns = "medicationId",
          onDelete = ForeignKey.CASCADE
      )
    },
    indices = {@Index("medicationId")}
)
public abstract class Prescription implements Parcelable, Comparable<Prescription> {

  public abstract int medicationId();
  @AutoValue.CopyAnnotations
  @NonNull
  public abstract LocalDate startDate();
  @AutoValue.CopyAnnotations
  @Nullable
  public abstract LocalDate endDate();

  public abstract String dosage();
  public abstract boolean takeInMorning();
  public abstract boolean takeAtNoon();
  public abstract boolean takeInEvening();
  public abstract boolean takeAtNight();
  public abstract boolean takeAsNeeded();

  public boolean active() {
    return expected() || takeAsNeeded();
  }

  public boolean expected() {
    return takeInMorning() || takeAtNoon() || takeInEvening() || takeAtNight();
  }

  public String getSummary() {
    String takeSummary = "";
    if (takeAsNeeded()) {
      takeSummary = "as needed";
    } else {
      List<String> parts = new ArrayList<>();
      if (takeInMorning()) {
        parts.add("in the morning");
      }
      if (takeAtNoon()) {
        parts.add("at noon");
      }
      if (takeInEvening()) {
        parts.add("in the evening");
      }
      if (takeAtNight()) {
        parts.add("at night");
      }
      takeSummary = Joiner.on(", ").join(parts);
    }
    return String.format("Starting %s, take %s", DateUtil.toUiStr(startDate()), takeSummary);
  }

  public static Prescription create(int medicationId, LocalDate startDate, LocalDate endDate, String dosage, boolean takeInMorning, boolean takeAtNoon, boolean takeInEvening, boolean takeAtNight, boolean takeAsNeeded) {
    return new AutoValue_Prescription(medicationId, startDate, endDate, dosage, takeInMorning, takeAtNoon, takeInEvening, takeAtNight, takeAsNeeded);
  }

  public boolean shouldTake(TimeOfDay timeOfDay) {
    switch (timeOfDay) {
      case MORNING:
        return takeInMorning();
      case NOON:
        return takeAtNoon();
      case EVENING:
        return takeInEvening();
      case NIGHT:
        return takeAtNight();
      case AS_NEEDED:
        return takeAsNeeded();
    }
    throw new IllegalStateException();
  }

  public enum TimeOfDay {
    MORNING, NOON, EVENING, NIGHT, AS_NEEDED
  }

  @Override
  public int compareTo(Prescription o) {
    return startDate().compareTo(o.startDate());
  }
}

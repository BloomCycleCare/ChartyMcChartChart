package com.bloomcyclecare.cmcc.data.models.medication;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.parceler.Parcel;

import java.util.Objects;

@Parcel
@Entity
public class Medication {
  @PrimaryKey(autoGenerate = true)
  public long id;
  public String name = "";
  public String description = "";
  public String dosage = "";
  public boolean takeInMorning;
  public boolean takeAtNoon;
  public boolean takeInEvening;
  public boolean takeAtNight;
  public boolean takeAsNeeded;

  public Medication() {}

  public Medication(Medication that) {
    this.id = that.id;
    this.name = that.name;
    this.description = that.description;
    this.dosage = that.dosage;
    this.takeInMorning = that.takeInMorning;
    this.takeInEvening = that.takeInEvening;
    this.takeAtNoon = that.takeAtNoon;
    this.takeAtNight = that.takeAtNight;
    this.takeAsNeeded = that.takeAsNeeded;
  }

  public boolean active() {
    return expected() || takeAsNeeded;
  }

  public boolean expected() {
    return takeInMorning || takeAtNoon || takeInEvening || takeAtNight;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Medication that = (Medication) o;
    return id == that.id &&
        takeAtNight == that.takeAtNight &&
        takeAtNoon == that.takeAtNoon &&
        takeInMorning == that.takeInMorning &&
        takeInEvening == that.takeInEvening &&
        takeAsNeeded == that.takeAsNeeded &&
        name.equals(that.name) &&
        description.equals(that.description) &&
        dosage.equals(that.dosage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, description, dosage, takeInMorning, takeInEvening, takeAtNight, takeAtNoon, takeAsNeeded);
  }

  public enum TimeOfDay {
    MORNING, NOON, EVENING, NIGHT, AS_NEEDED
  }

  public boolean shouldTake(TimeOfDay timeOfDay) {
    switch (timeOfDay) {
      case MORNING:
        return takeInMorning;
      case NOON:
        return takeAtNoon;
      case EVENING:
        return takeInEvening;
      case NIGHT:
        return takeAtNight;
      case AS_NEEDED:
        return takeAsNeeded;
    }
    throw new IllegalStateException();
  }
}

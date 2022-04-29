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
  public boolean active;
  public String name = "";
  public String description = "";
  public String dosage = "";
  public boolean takeInMorning;
  public boolean takeAtNoon;
  public boolean takeInEvening;
  public boolean takeAtNight;
  @Deprecated public String frequency = "";

  public Medication() {}

  public Medication(Medication that) {
    this.id = that.id;
    this.active = that.active;
    this.name = that.name;
    this.description = that.description;
    this.dosage = that.dosage;
    this.takeInMorning = that.takeInMorning;
    this.takeInEvening = that.takeInEvening;
    this.takeAtNoon = that.takeAtNoon;
    this.takeAtNight = that.takeAtNight;
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
        active == that.active &&
        takeAtNight == that.takeAtNight &&
        takeAtNoon == that.takeAtNoon &&
        takeInMorning == that.takeInMorning &&
        takeInEvening == that.takeInEvening &&
        name.equals(that.name) &&
        description.equals(that.description) &&
        dosage.equals(that.dosage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, description, dosage, active, takeInMorning, takeInEvening, takeAtNight, takeAtNoon);
  }

  public enum TimeOfDay {
    MORNING, NOON, EVENING, NIGHT
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
    }
    throw new IllegalStateException();
  }
}

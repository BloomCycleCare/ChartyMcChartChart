package com.bloomcyclecare.cmcc.data.models.medication;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.parceler.Parcel;

import java.util.Objects;

@Parcel
@Entity
public class Medication {
  @PrimaryKey(autoGenerate = true)
  public long medicationId;
  public boolean active;
  public String name = "";
  public String description = "";
  public String dosage = "";
  public String frequency = "";

  public Medication() {}

  public Medication(Medication that) {
    this.medicationId = that.medicationId;
    this.active = that.active;
    this.name = that.name;
    this.description = that.description;
    this.dosage = that.dosage;
    this.frequency = that.frequency;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Medication that = (Medication) o;
    return medicationId == that.medicationId &&
        active == that.active &&
        name.equals(that.name) &&
        description.equals(that.description) &&
        dosage.equals(that.dosage) &&
        frequency.equals(that.frequency);
  }

  @Override
  public int hashCode() {
    return Objects.hash(medicationId, name, description, dosage, frequency, active);
  }
}

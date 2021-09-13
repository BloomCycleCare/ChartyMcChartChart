package com.bloomcyclecare.cmcc.data.models.medication;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.parceler.Parcel;

import java.util.Objects;

@Parcel
@Entity
public class Medication {
  @PrimaryKey(autoGenerate = true)
  public int id;
  public boolean active;
  public String name = "";
  public String description = "";
  public String dosage = "";
  public String frequency = "";

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Medication that = (Medication) o;
    return id == that.id &&
        active == that.active &&
        name.equals(that.name) &&
        description.equals(that.description) &&
        dosage.equals(that.dosage) &&
        frequency.equals(that.frequency);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, description, dosage, frequency, active);
  }
}

package com.bloomcyclecare.cmcc.data.models.medication;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.parceler.Parcel;

@Parcel
@Entity
public class Medication {
  @PrimaryKey(autoGenerate = true)
  public int id;
  public String name;
  public String description;
  public String dosage;
  public String frequency;
  public boolean active;
}

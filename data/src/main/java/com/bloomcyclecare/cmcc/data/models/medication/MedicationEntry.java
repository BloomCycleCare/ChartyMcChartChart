package com.bloomcyclecare.cmcc.data.models.medication;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import com.bloomcyclecare.cmcc.data.models.Entry;
import com.google.common.collect.ImmutableList;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.parceler.Parcel;

import java.util.List;

@Parcel
public class MedicationEntry implements Entry {
  @Embedded
  BaseMedicationEntry medicationEntry;

  @Relation(
      parentColumn = "entryDate",
      entityColumn = "medicationId",
      associateBy = @Junction(MedicationRef.class)
  )
  public List<Medication> medications;

  public static MedicationEntry emptyEntry(LocalDate date) {
    MedicationEntry entry = new MedicationEntry();
    entry.medicationEntry = BaseMedicationEntry.emptyEntry(date);
    entry.medications = ImmutableList.of();
    return entry;
  }

  @Override
  public LocalDate getDate() {
    return medicationEntry.getDate();
  }

  @Override
  public DateTime timeCreated() {
    return medicationEntry.timeCreated();
  }

  @Override
  public void setTimeCreated(DateTime dateTime) {
    medicationEntry.setTimeCreated(dateTime);
  }

  @Override
  public DateTime timeUpdated() {
    return medicationEntry.timeUpdated();
  }

  @Override
  public void setTimeUpdated(DateTime dateTime) {
    medicationEntry.setTimeUpdated(dateTime);
  }

  @Override
  public int timesUpdated() {
    return medicationEntry.timesUpdated();
  }
}

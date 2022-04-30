package com.bloomcyclecare.cmcc.data.models.medication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import com.bloomcyclecare.cmcc.data.models.Entry;

import org.joda.time.LocalDate;
import org.parceler.Parcel;

@Parcel
@Entity(
    primaryKeys = {"entryDate", "medicationId", "time"},
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
public class MedicationRef {
  @NonNull
  public LocalDate entryDate;
  public int medicationId;
  @NonNull
  public Medication.TimeOfDay time;

  public static MedicationRef create(Entry entry, Medication medication, Medication.TimeOfDay time) {
    MedicationRef ref = new MedicationRef();
    ref.entryDate = entry.getDate();
    ref.medicationId = (int) medication.id;
    ref.time = time;
    return ref;
  }
}

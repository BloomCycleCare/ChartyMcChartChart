package com.bloomcyclecare.cmcc.data.models.medication;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import org.joda.time.LocalDate;

@Entity(
    primaryKeys = {"entryDate", "medicationId"},
    foreignKeys = {
        @ForeignKey(
            entity = Medication.class,
            parentColumns = "medicationId",
            childColumns = "medicationId",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {@Index("medicationId")}
)
public class MedicationRef {
  @NonNull public LocalDate entryDate;
  public int medicationId;
}

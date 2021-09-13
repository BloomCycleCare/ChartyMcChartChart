package com.bloomcyclecare.cmcc.data.models.medication;

import androidx.room.Entity;
import androidx.room.ForeignKey;

import org.joda.time.LocalDate;

@Entity(
    primaryKeys = {"entryDate", "medicationId"},
    foreignKeys = {
        @ForeignKey(
            entity = Medication.class,
            parentColumns = "id",
            childColumns = "medicationId",
            onDelete = ForeignKey.CASCADE
        )
    }
)
public class MedicationRef {
  public LocalDate entryDate;
  public int medicationId;
}

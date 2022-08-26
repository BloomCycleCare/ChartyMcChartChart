package com.bloomcyclecare.cmcc.data.models.medication;

import android.os.Parcelable;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@AutoValue
public abstract class MedicationWithRelations implements Parcelable, Comparable<MedicationWithRelations> {

  @AutoValue.CopyAnnotations
  @Embedded
  public abstract Medication medication();


  @AutoValue.CopyAnnotations
  @Relation(parentColumn = "id", entityColumn = "medicationId")
  public abstract List<Prescription> prescriptions();

  public static MedicationWithRelations create(Medication medication, List<Prescription> prescriptions) {
    return new AutoValue_MedicationWithRelations(medication, prescriptions);
  }

  public boolean hasActivePrescription() {
    return prescriptions().stream().anyMatch(p -> p.endDate() == null);
  }

  @Override
  public int compareTo(MedicationWithRelations o) {
    if (hasActivePrescription() == o.hasActivePrescription()) {
      return 0;
    }
    if (hasActivePrescription()) {
      return 1;
    }
    return -1;
  }
}

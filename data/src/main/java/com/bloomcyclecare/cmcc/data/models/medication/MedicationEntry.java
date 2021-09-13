package com.bloomcyclecare.cmcc.data.models.medication;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Relation;

import com.bloomcyclecare.cmcc.data.models.Entry;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;
import org.parceler.Parcel;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Parcel
public class MedicationEntry extends Entry {

  @Relation(entity = Medication.class)
  @NonNull public List<Integer> medications = new ArrayList<>();

  public MedicationEntry() {
    super();
  }

  private MedicationEntry(LocalDate date) {
    super(date);
  }

  @Ignore
  public MedicationEntry(@NonNull Entry entry, @NonNull Collection<Integer> medications) {
    super(entry);
    this.medications.addAll(medications);
  }

  @Override
  public List<String> getSummaryLines() {
    return ImmutableList.of();
  }

  public static MedicationEntry emptyEntry(LocalDate date) {
    return new MedicationEntry(date);
  }
}

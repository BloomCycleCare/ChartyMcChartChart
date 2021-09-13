package com.bloomcyclecare.cmcc.data.models.medication;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Junction;
import androidx.room.Relation;

import com.bloomcyclecare.cmcc.data.models.Entry;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;
import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Parcel
public class MedicationEntry extends Entry {

  public MedicationEntry() {
    super();
  }

  private MedicationEntry(LocalDate date) {
    super(date);
  }

  @Ignore
  public MedicationEntry(@NonNull Entry entry) {
    super(entry);
  }

  @Override
  public List<String> getSummaryLines() {
    return ImmutableList.of();
  }

  public static MedicationEntry emptyEntry(LocalDate date) {
    return new MedicationEntry(date);
  }
}

class DecoratedMedicationEntry {
  @Embedded MedicationEntry medicationEntry;
  @Relation(
      parentColumn = "entryDate",
      entityColumn = "entryDate",
      associateBy = @Junction(MedicationRef.class)
  )
  public List<Medication> medications;
}

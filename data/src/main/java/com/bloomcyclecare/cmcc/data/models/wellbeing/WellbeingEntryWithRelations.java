package com.bloomcyclecare.cmcc.data.models.wellbeing;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.bloomcyclecare.cmcc.data.models.Sumarizable;
import com.bloomcyclecare.cmcc.data.models.medication.MedicationRef;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.List;

@Parcel
public class WellbeingEntryWithRelations implements Sumarizable {
  @Embedded public WellbeingEntry wellbeingEntry;

  @Relation(parentColumn = "entryDate", entityColumn = "entryDate")
  public List<MedicationRef> medicationRefs = new ArrayList<>();

  public WellbeingEntryWithRelations() {}

  public static WellbeingEntryWithRelations create(WellbeingEntry entry, List<MedicationRef> refs) {
    WellbeingEntryWithRelations out = new WellbeingEntryWithRelations();
    out.wellbeingEntry = entry;
    out.medicationRefs = refs;
    return out;
  };

  @Override
  public List<String> getSummaryLines() {
    List<String> lines = new ArrayList<>(wellbeingEntry.getSummaryLines());
    if (!medicationRefs.isEmpty()) {
      lines.add(String.format("Took %d medications", medicationRefs.size()));
    }
    return lines;
  }
}

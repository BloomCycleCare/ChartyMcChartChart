package com.bloomcyclecare.cmcc.data.db;

import androidx.room.Dao;

import com.bloomcyclecare.cmcc.data.models.medication.MedicationEntry;

@Dao
public abstract class MedicationEntryDao extends BaseEntryDao<MedicationEntry> {
  public MedicationEntryDao() {
    super(MedicationEntry.class, MedicationEntry::emptyEntry);
  }


}

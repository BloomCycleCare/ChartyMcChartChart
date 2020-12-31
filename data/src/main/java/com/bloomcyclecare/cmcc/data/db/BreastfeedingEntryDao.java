package com.bloomcyclecare.cmcc.data.db;

import com.bloomcyclecare.cmcc.data.models.breastfeeding.BreastfeedingEntry;

import androidx.room.Dao;

@Dao
public abstract class BreastfeedingEntryDao extends BaseEntryDao<BreastfeedingEntry> {
  public BreastfeedingEntryDao() {
    super(BreastfeedingEntry.class, BreastfeedingEntry::emptyEntry);
  }
}

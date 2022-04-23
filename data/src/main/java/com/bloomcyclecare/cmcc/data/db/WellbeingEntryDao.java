package com.bloomcyclecare.cmcc.data.db;

import androidx.room.Dao;

import com.bloomcyclecare.cmcc.data.models.wellbeing.WellbeingEntry;

@Dao
public abstract class WellbeingEntryDao extends BaseEntryDao<WellbeingEntry> {
  public WellbeingEntryDao() {
    super(WellbeingEntry.class, WellbeingEntry::emptyEntry);
  }
}

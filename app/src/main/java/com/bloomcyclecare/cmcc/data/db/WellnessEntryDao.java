package com.bloomcyclecare.cmcc.data.db;


import com.bloomcyclecare.cmcc.data.entities.WellnessEntry;

import androidx.room.Dao;

@Dao
public abstract class WellnessEntryDao extends BaseEntryDao<WellnessEntry> {

  public WellnessEntryDao() {
    super(WellnessEntry.class, WellnessEntry::emptyEntry);
  }
}

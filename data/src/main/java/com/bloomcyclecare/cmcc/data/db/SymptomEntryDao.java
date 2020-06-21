package com.bloomcyclecare.cmcc.data.db;


import com.bloomcyclecare.cmcc.data.models.observation.SymptomEntry;

import androidx.room.Dao;

@Dao
public abstract class SymptomEntryDao extends BaseEntryDao<SymptomEntry> {

  public SymptomEntryDao() {
    super(SymptomEntry.class, SymptomEntry::emptyEntry);
  }
}

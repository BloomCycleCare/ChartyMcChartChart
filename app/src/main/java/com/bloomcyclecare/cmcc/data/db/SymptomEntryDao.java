package com.bloomcyclecare.cmcc.data.db;


import com.bloomcyclecare.cmcc.data.entities.SymptomEntry;

import androidx.room.Dao;

@Dao
public abstract class SymptomEntryDao extends BaseEntryDao<SymptomEntry> {

  public SymptomEntryDao() {
    super(SymptomEntry.class, SymptomEntry::emptyEntry);
  }
}

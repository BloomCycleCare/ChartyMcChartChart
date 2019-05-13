package com.roamingroths.cmcc.data.db;


import androidx.room.Dao;

import com.roamingroths.cmcc.data.entities.SymptomEntry;

@Dao
public abstract class SymptomEntryDao extends BaseEntryDao<SymptomEntry> {

  public SymptomEntryDao() {
    super(SymptomEntry.class, SymptomEntry::emptyEntry);
  }
}

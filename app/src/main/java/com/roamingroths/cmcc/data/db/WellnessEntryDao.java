package com.roamingroths.cmcc.data.db;


import androidx.room.Dao;

import com.roamingroths.cmcc.data.entities.SymptomEntry;
import com.roamingroths.cmcc.data.entities.WellnessEntry;

@Dao
public abstract class WellnessEntryDao extends BaseEntryDao<WellnessEntry> {

  public WellnessEntryDao() {
    super(WellnessEntry.class);
  }

}

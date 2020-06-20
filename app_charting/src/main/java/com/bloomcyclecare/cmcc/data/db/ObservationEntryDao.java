package com.bloomcyclecare.cmcc.data.db;


import com.bloomcyclecare.cmcc.data.entities.ObservationEntry;

import androidx.room.Dao;

@Dao
public abstract class ObservationEntryDao extends BaseEntryDao<ObservationEntry> {

  public ObservationEntryDao() {
    super(ObservationEntry.class, ObservationEntry::emptyEntry);
  }
}

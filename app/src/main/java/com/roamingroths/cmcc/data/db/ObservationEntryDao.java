package com.roamingroths.cmcc.data.db;


import androidx.room.Dao;

import com.roamingroths.cmcc.data.entities.ObservationEntry;

@Dao
public abstract class ObservationEntryDao extends BaseEntryDao<ObservationEntry> {

  public ObservationEntryDao() {
    super(ObservationEntry.class, ObservationEntry::emptyEntry);
  }
}

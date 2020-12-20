package com.bloomcyclecare.cmcc.data.db;

import com.bloomcyclecare.cmcc.data.models.measurement.MeasurementEntry;

import androidx.room.Dao;

@Dao
public abstract class MeasurementEntryDao extends BaseEntryDao<MeasurementEntry>  {

  public MeasurementEntryDao() {
    super(MeasurementEntry.class, MeasurementEntry::emptyEntry);
  }
}

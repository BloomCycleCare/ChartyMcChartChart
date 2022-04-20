package com.bloomcyclecare.cmcc.data.db;

import androidx.room.Dao;

import com.bloomcyclecare.cmcc.data.models.breastfeeding.BreastfeedingEntry;
import com.bloomcyclecare.cmcc.data.models.lifestyle.LifestyleEntry;

@Dao
public abstract class LifestyleEntryDao extends BaseEntryDao<LifestyleEntry> {
  public LifestyleEntryDao() {
    super(LifestyleEntry.class, LifestyleEntry::emptyEntry);
  }
}

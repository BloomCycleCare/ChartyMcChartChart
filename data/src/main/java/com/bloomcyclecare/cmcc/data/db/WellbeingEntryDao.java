package com.bloomcyclecare.cmcc.data.db;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import com.bloomcyclecare.cmcc.data.models.wellbeing.WellbeingEntry;
import com.bloomcyclecare.cmcc.data.models.wellbeing.WellbeingEntryWithRelations;

import org.joda.time.LocalDate;

@Dao
public abstract class WellbeingEntryDao extends BaseEntryDao<WellbeingEntry> {
  public WellbeingEntryDao() {
    super(WellbeingEntry.class, WellbeingEntry::emptyEntry);
  }

  @Transaction
  @Query("SELECT * FROM WellbeingEntry WHERE entryDate=:entryDate")
  public abstract WellbeingEntryWithRelations entryWithRelations(LocalDate entryDate);
}

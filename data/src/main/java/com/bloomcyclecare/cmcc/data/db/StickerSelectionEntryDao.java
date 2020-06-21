package com.bloomcyclecare.cmcc.data.db;


import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelectionEntry;

import androidx.room.Dao;

@Dao
public abstract class StickerSelectionEntryDao extends BaseEntryDao<StickerSelectionEntry> {

  public StickerSelectionEntryDao() {
    super(StickerSelectionEntry.class, StickerSelectionEntry::emptyEntry);
  }
}

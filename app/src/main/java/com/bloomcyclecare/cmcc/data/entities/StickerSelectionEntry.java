package com.bloomcyclecare.cmcc.data.entities;

import com.bloomcyclecare.cmcc.data.models.StickerSelection;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;

import java.util.List;

import androidx.room.Entity;

@Entity
public class StickerSelectionEntry extends Entry {

  public StickerSelection selection;

  public StickerSelectionEntry(LocalDate entryDate, StickerSelection selection) {
    super(entryDate);
    this.selection = selection;
  }

  public static StickerSelectionEntry emptyEntry(LocalDate entryDate) {
    return new StickerSelectionEntry(entryDate, StickerSelection.empty());
  }

  @Override
  public List<String> getSummaryLines() {
    return ImmutableList.of();
  }
}

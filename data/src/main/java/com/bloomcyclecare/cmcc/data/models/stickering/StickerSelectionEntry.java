package com.bloomcyclecare.cmcc.data.models.stickering;

import com.bloomcyclecare.cmcc.data.models.Entry;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Objects;

import androidx.room.Entity;

@Entity
public class StickerSelectionEntry extends Entry {

  public StickerSelection selection;

  public StickerSelectionEntry() {
    super();
  }

  public StickerSelectionEntry(Entry entry, StickerSelection selection) {
    super(entry);
    this.selection = selection;
  }

  public static StickerSelectionEntry emptyEntry(LocalDate entryDate) {
    return new StickerSelectionEntry(entryDate);
  }

  private StickerSelectionEntry(LocalDate date) {
    super(date);
    this.selection = StickerSelection.empty();
  }

  @Override
  public List<String> getSummaryLines() {
    return ImmutableList.of();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StickerSelectionEntry that = (StickerSelectionEntry) o;
    return getDate().equals(that.getDate()) && Objects.equals(selection, that.selection);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getDate(), selection);
  }
}

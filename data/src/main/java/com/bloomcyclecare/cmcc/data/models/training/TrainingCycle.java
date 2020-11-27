package com.bloomcyclecare.cmcc.data.models.training;

import com.bloomcyclecare.cmcc.data.models.instructions.Instructions;
import com.google.common.collect.ImmutableMap;

import java.util.LinkedHashMap;
import java.util.Optional;

public class TrainingCycle {

  public final Instructions instructions;
  private final LinkedHashMap<TrainingEntry, Optional<StickerExpectations>> entries = new LinkedHashMap<>();
  private String title = "";
  private String subtitle = "";

  private TrainingCycle(Instructions instructions) {
    this.instructions = instructions;
  }

  public static TrainingCycle withInstructions(Instructions instructions) {
    return new TrainingCycle(instructions);
  }

  public TrainingCycle withTitle(String title) {
    this.title = title;
    return this;
  }

  public TrainingCycle withSubtitle(String subtitle) {
    this.subtitle = subtitle;
    return this;
  }

  public TrainingCycle addEntry(TrainingEntry entry, StickerExpectations expectations) {
    entries.put(entry, Optional.of(expectations));
    return this;
  }

  public TrainingCycle addEntry(TrainingEntry entry) {
    entries.put(entry, Optional.empty());
    return this;
  }

  public TrainingCycle addEntriesFrom(TrainingCycle other) {
    entries.putAll(other.entries());
    return this;
  }

  public ImmutableMap<TrainingEntry, Optional<StickerExpectations>> entries() {
    return ImmutableMap.copyOf(entries);
  }

}

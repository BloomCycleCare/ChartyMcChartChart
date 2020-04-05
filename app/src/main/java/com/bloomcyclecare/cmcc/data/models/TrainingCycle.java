package com.bloomcyclecare.cmcc.data.models;

import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.bloomcyclecare.cmcc.logic.chart.StickerColor;
import com.google.common.collect.ImmutableMap;

import java.util.LinkedHashMap;

public class TrainingCycle {

  public final Instructions instructions;
  private final LinkedHashMap<TrainingEntry, StickerExpectations> entries = new LinkedHashMap<>();

  private TrainingCycle(Instructions instructions) {
    this.instructions = instructions;
  }

  public static TrainingCycle withInstructions(Instructions instructions) {
    return new TrainingCycle(instructions);
  }

  public void addEntry(TrainingEntry entry, StickerExpectations expectations) {
    entries.put(entry, expectations);
  }

  public ImmutableMap<TrainingEntry, StickerExpectations> entries() {
    return ImmutableMap.copyOf(entries);
  }

  public static class StickerExpectations {

    public final StickerColor backgroundColor;
    public boolean shouldHaveBaby = false;
    public boolean shouldHaveIntercourse = false;
    public String peakText = "";

    private StickerExpectations(StickerColor backgroundColor) {
      this.backgroundColor = backgroundColor;
    }

    public static StickerExpectations redSticker() {
      return new StickerExpectations(StickerColor.RED);
    }

    public static StickerExpectations greenSticker() {
      return new StickerExpectations(StickerColor.GREEN);
    }

    public static StickerExpectations yellowSticker() {
      return new StickerExpectations(StickerColor.YELLOW);
    }

    public static StickerExpectations whiteSticker() {
      return new StickerExpectations(StickerColor.WHITE);
    }

    public static StickerExpectations greySticker() {
      return new StickerExpectations(StickerColor.GREY);
    }

    public StickerExpectations withPeakText(String text) {
      peakText = text;
      return this;
    }

    public StickerExpectations withBaby() {
      shouldHaveBaby = true;
      return this;
    }

    public StickerExpectations withIntercourse() {
      shouldHaveIntercourse = true;
      return this;
    }
  }
}

package com.bloomcyclecare.cmcc.data.models;

import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;

import java.util.List;

public class TrainingCycles {

  public static TrainingCycle basicB1A() {
    TrainingCycle cycle = TrainingCycle.withInstructions(Instructions.createBasicInstructions(LocalDate.now()));
    cycle.addEntry(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker());
    cycle.addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker());
    cycle.addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker());
    cycle.addEntry(TrainingEntry.forText("L 0AD"), TrainingCycle.StickerExpectations.redSticker());
    cycle.addEntry(TrainingEntry.forText("2x1"), TrainingCycle.StickerExpectations.greenSticker());
    cycle.addEntry(TrainingEntry.forText("0AD"), TrainingCycle.StickerExpectations.greenSticker());
    cycle.addEntry(TrainingEntry.forText("0AD"), TrainingCycle.StickerExpectations.greenSticker());

    cycle.addEntry(TrainingEntry.forText("0AD"), TrainingCycle.StickerExpectations.greenSticker());
    cycle.addEntry(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    cycle.addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    cycle.addEntry(TrainingEntry.forText("8kx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    cycle.addEntry(TrainingEntry.forText("10klAD"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    cycle.addEntry(TrainingEntry.forText("10klAD").peakDay(), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("P"));
    cycle.addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("1"));

    cycle.addEntry(TrainingEntry.forText("0AD"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("2"));
    cycle.addEntry(TrainingEntry.forText("4x1"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("3"));
    cycle.addEntry(TrainingEntry.forText("0AD"), TrainingCycle.StickerExpectations.greenSticker());
    cycle.addEntry(TrainingEntry.forText("0AD"), TrainingCycle.StickerExpectations.greenSticker());
    cycle.addEntry(TrainingEntry.forText("2x1"), TrainingCycle.StickerExpectations.greenSticker());
    return cycle;
  }

  public static TrainingCycle basicB1B() {
    TrainingCycle cycle = TrainingCycle.withInstructions(Instructions.createBasicInstructions(LocalDate.now()));
    cycle.addEntry(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker());
    cycle.addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker());
    cycle.addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker());
    cycle.addEntry(TrainingEntry.forText("L0AD"), TrainingCycle.StickerExpectations.redSticker());
    cycle.addEntry(TrainingEntry.forText("VL2x1"), TrainingCycle.StickerExpectations.redSticker());
    cycle.addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker());
    cycle.addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker());

    cycle.addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    cycle.addEntry(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    cycle.addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker());
    cycle.addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker());
    cycle.addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker());
    cycle.addEntry(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    cycle.addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker());

    cycle.addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    cycle.addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    cycle.addEntry(TrainingEntry.forText("8cad"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    cycle.addEntry(TrainingEntry.forText("10cx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    cycle.addEntry(TrainingEntry.forText("10klad"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    cycle.addEntry(TrainingEntry.forText("10klad").peakDay(), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("P"));
    cycle.addEntry(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("1"));

    cycle.addEntry(TrainingEntry.forText("4x1"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("2"));
    cycle.addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("3"));
    cycle.addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker());
    cycle.addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker());
    cycle.addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker());
    cycle.addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker());
    cycle.addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker());
    return  cycle;
  }

  public static List<TrainingCycle> basicTrainingCycles() {
    return ImmutableList.of(basicB1A(), basicB1B());
  }
}

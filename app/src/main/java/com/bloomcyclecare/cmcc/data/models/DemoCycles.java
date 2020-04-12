package com.bloomcyclecare.cmcc.data.models;

import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;

import java.util.List;

public class DemoCycles {

  public static TrainingCycle BASIC_B1A = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now()))
      .addEntry(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("L 0AD"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("2x1"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0AD"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0AD"), TrainingCycle.StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("0AD"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("8kx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10klAD"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10klAD").peakDay(), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("P"))
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("1"))

      .addEntry(TrainingEntry.forText("0AD"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("2"))
      .addEntry(TrainingEntry.forText("4x1"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("3"))
      .addEntry(TrainingEntry.forText("0AD"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0AD"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("2x1"), TrainingCycle.StickerExpectations.greenSticker());

  public static TrainingCycle BASIC_B1B = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now()))
      .addEntry(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("L0AD"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("VL2x1"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("8cad"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10cx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10klad"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10klad").peakDay(), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("P"))
      .addEntry(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("1"))

      .addEntry(TrainingEntry.forText("4x1"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("2"))
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("3"))
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker());

  public static TrainingCycle BASIC_B1C = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now()))
      .addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("L0AD"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("2x2"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("4x1"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("1"))
      .addEntry(TrainingEntry.forText("2x2"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("2"))
      .addEntry(TrainingEntry.forText("4x1"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("3"))

      .addEntry(TrainingEntry.forText("4x1"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10kad"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10klad"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10wlx2").peakDay(), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("P"))
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("1"))

      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("2"))
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("3"))
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker());

  public static TrainingCycle BASIC_B1D = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now()))
      .addEntry(TrainingEntry.forText("L0AD"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("L8CX1"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10kx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())

      .addEntry(TrainingEntry.forText("10klad"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10cx1").peakDay(), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("P"))
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("1"))
      .addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("2"))
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("3"))
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("4x2"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("4x1"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("4ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("4ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("2x1"), TrainingCycle.StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker());

  public static TrainingCycle BASIC_B1E = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now()))
      .addEntry(TrainingEntry.forText("L0AD"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("L2x2"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("10cx1").peakDay(), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("P"))

      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("1"))
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("2"))
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("3"))
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())

      .addEntry(TrainingEntry.forText("8kx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10klx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10cx1").peakDay(), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("P"))
      .addEntry(TrainingEntry.forText("2x2"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("1"))
      .addEntry(TrainingEntry.forText("4x1"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("2"))
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("3"))
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker());

  public static TrainingCycle BASIC_B1F = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now()))
      .addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("L0AD"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("4x1"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("8kx2").peakDay(), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("P"))
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("1"))
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("2"))

      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("3"))
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("vl10kx2").unusualBleeding(), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("vl0ad").unusualBleeding(), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("1"))
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("2"))

      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("3"))
      .addEntry(TrainingEntry.forText("2x1"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("4x2"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("4x1"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker());

  public static List<TrainingCycle> basicTrainingCycles() {
    return ImmutableList.of(BASIC_B1A, BASIC_B1B, BASIC_B1C, BASIC_B1D, BASIC_B1E, BASIC_B1F);
  }

  public static List<TrainingCycle> forRepos() {
    return basicTrainingCycles();
  }
}

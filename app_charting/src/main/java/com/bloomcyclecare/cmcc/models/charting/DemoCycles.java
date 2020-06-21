package com.bloomcyclecare.cmcc.models.charting;

import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.bloomcyclecare.cmcc.models.instructions.BasicInstruction;
import com.bloomcyclecare.cmcc.models.instructions.YellowStampInstruction;
import com.bloomcyclecare.cmcc.models.training.TrainingCycle;
import com.bloomcyclecare.cmcc.models.training.TrainingEntry;
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

  public static TrainingCycle BASIC_B7A = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now()))
      .addEntry(TrainingEntry.forText("L2AD"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("L2AD"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("2AD").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse())
      .addEntry(TrainingEntry.forText("2AD"), TrainingCycle.StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("2AD").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse())
      .addEntry(TrainingEntry.forText("2AD").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse())
      .addEntry(TrainingEntry.forText("2AD"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("8kad"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10cx1").peakDay(), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("P"))

      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("1"))
      .addEntry(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("2"))
      .addEntry(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("3"))
      .addEntry(TrainingEntry.forText("4ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("2ad").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse())
      .addEntry(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("10cklx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())

      .addEntry(TrainingEntry.forText("10ckad").peakDay(), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("P"))
      .addEntry(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("1"))
      .addEntry(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("2"))
      .addEntry(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("3"))
      .addEntry(TrainingEntry.forText("2x3").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse())

      .addEntry(TrainingEntry.forText("4x1").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse())
      .addEntry(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("2ad").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse())
      .addEntry(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker());

  public static TrainingCycle POST_PEAK_YELLOW_STAMPS = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now()))
      .addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("L2ad"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("VL0ad"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("0ad").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("2ad").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse())

      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10cgx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10cklx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10klx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10cklad"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("4x2"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("1"))

      .addEntry(TrainingEntry.forText("10cx1").peakDay(), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("P"))
      .addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("1"))
      .addEntry(TrainingEntry.forText("8cgx2"), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("2"))
      .addEntry(TrainingEntry.forText("8gyx1"), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("3"))
      .addEntry(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker())

      .addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("4ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.yellowSticker());

  // From "Use of Pre-Peak Yellow Stamps in Regular Cycles (21 to 38 Days)
  // with a Mucus Cycle more thn 8 Days"
  public static TrainingCycle PRE_PEAK_YELLOW_STAMPS = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now())
          .addInstructions(BasicInstruction.K_1))
      .addEntry(TrainingEntry.forText("L2ad"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("L6cx1"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("L6cx2"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("VL6cx2"), TrainingCycle.StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.yellowSticker())

      .addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8kx2").pointOfChange(), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10kx3"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10klx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())

      .addEntry(TrainingEntry.forText("10klx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10wlad"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("10wlad").peakDay(), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("P"))
      .addEntry(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("1"))
      .addEntry(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("2"))
      .addEntry(TrainingEntry.forText("4x1"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("3"))
      .addEntry(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker());

  // From Book 2, figure 6-3: The use of pre-Peak yellow stamps in a women who is breastfeeding
  public static TrainingCycle BREASTFEEDING_PRE_PEAK_YELLOW_STAMPS = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now())
          .addInstructions(BasicInstruction.K_1)
          .addInstructions(
              YellowStampInstruction.YS_1_A, YellowStampInstruction.YS_1_B,
              YellowStampInstruction.YS_1_C, YellowStampInstruction.YS_1_D))
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6gcx1"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8gyx2"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker())

      // Starting at 8
      .addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cad"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.yellowSticker())

      // Starting at 15
      .addEntry(TrainingEntry.forText("8gyx2"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8kx2").pointOfChange(), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("6cx1").pointOfChange(), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("1"))
      .addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("2"))
      .addEntry(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("3"))
      .addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.yellowSticker())

      // Starting at 22
      .addEntry(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("10kad").pointOfChange(), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("6cx2").pointOfChange(), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("1"))
      .addEntry(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("2"))
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("3"))
      .addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.yellowSticker())

      // Starting at 29
      .addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cgx1"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.yellowSticker())

      // Starting at 36
      .addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cad").pointOfChange(), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("8kx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("8kx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby())
      .addEntry(TrainingEntry.forText("6cx2").pointOfChange(), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("1"))

      // Starting at 43
      .addEntry(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("2"))
      .addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("3"))
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker());

  public static List<TrainingCycle> basicTrainingCycles() {
    return ImmutableList.of(BASIC_B1A, BASIC_B1B, BASIC_B1C, BASIC_B1D, BASIC_B1E, BASIC_B1F, BASIC_B7A);
  }

  public static List<TrainingCycle> forRepos() {
    return basicTrainingCycles();
  }
}

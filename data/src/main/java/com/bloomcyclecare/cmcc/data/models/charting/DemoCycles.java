package com.bloomcyclecare.cmcc.data.models.charting;

import com.bloomcyclecare.cmcc.data.models.instructions.BasicInstruction;
import com.bloomcyclecare.cmcc.data.models.instructions.Instructions;
import com.bloomcyclecare.cmcc.data.models.instructions.YellowStampInstruction;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerText;
import com.bloomcyclecare.cmcc.data.models.training.StickerExpectations;
import com.bloomcyclecare.cmcc.data.models.training.TrainingCycle;
import com.bloomcyclecare.cmcc.data.models.training.TrainingEntry;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;

import java.util.List;

public class DemoCycles {

  public static TrainingCycle BASIC_B1A = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now()))
      .addEntry(TrainingEntry.forText("H"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("L 0AD"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("2x1"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0AD"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0AD"), StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("0AD"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("6cx2"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("8cx2"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("8kx2"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10klAD"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10klAD").peakDay(), StickerExpectations.whiteBabySticker(StickerText.P))
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.whiteBabySticker(StickerText.ONE))

      .addEntry(TrainingEntry.forText("0AD"), StickerExpectations.greenBabySticker(StickerText.TWO))
      .addEntry(TrainingEntry.forText("4x1"), StickerExpectations.greenBabySticker(StickerText.THREE))
      .addEntry(TrainingEntry.forText("0AD"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0AD"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("2x1"), StickerExpectations.greenSticker());

  public static TrainingCycle BASIC_B1B = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now()))
      .addEntry(TrainingEntry.forText("H"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("L0AD"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("VL2x1"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("6cx2"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("8cx1"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("8cx2"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("8cad"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10cx2"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10klad"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10klad").peakDay(), StickerExpectations.whiteBabySticker(StickerText.P))
      .addEntry(TrainingEntry.forText("8cx1"), StickerExpectations.whiteBabySticker(StickerText.ONE))

      .addEntry(TrainingEntry.forText("4x1"), StickerExpectations.greenBabySticker(StickerText.TWO))
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenBabySticker(StickerText.THREE))
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker());

  public static TrainingCycle BASIC_B1C = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now()))
      .addEntry(TrainingEntry.forText("M"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("H"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("L0AD"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("2x2"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("4x1"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("2ad"), StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("8cx1"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenBabySticker(StickerText.ONE))
      .addEntry(TrainingEntry.forText("2x2"), StickerExpectations.greenBabySticker(StickerText.TWO))
      .addEntry(TrainingEntry.forText("4x1"), StickerExpectations.greenBabySticker(StickerText.THREE))

      .addEntry(TrainingEntry.forText("4x1"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("8cx2"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10kad"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10klad"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10wlx2").peakDay(), StickerExpectations.whiteBabySticker(StickerText.P))
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenBabySticker(StickerText.ONE))

      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenBabySticker(StickerText.TWO))
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenBabySticker(StickerText.THREE))
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker());

  public static TrainingCycle BASIC_B1D = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now()))
      .addEntry(TrainingEntry.forText("L0AD"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("H"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("L8CX1"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("8cx2"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10cx1"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10kx2"), StickerExpectations.whiteBabySticker(null))

      .addEntry(TrainingEntry.forText("10klad"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10cx1").peakDay(), StickerExpectations.whiteBabySticker(StickerText.P))
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.whiteBabySticker(StickerText.ONE))
      .addEntry(TrainingEntry.forText("8cx2"), StickerExpectations.whiteBabySticker(StickerText.TWO))
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenBabySticker(StickerText.THREE))
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("4x2"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("4x1"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("4ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("4ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("2x1"), StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker());

  public static TrainingCycle BASIC_B1E = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now()))
      .addEntry(TrainingEntry.forText("L0AD"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("H"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("L2x2"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("10cx1").peakDay(), StickerExpectations.whiteBabySticker(StickerText.P))

      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenBabySticker(StickerText.ONE))
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenBabySticker(StickerText.TWO))
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenBabySticker(StickerText.THREE))
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("8cx1"), StickerExpectations.whiteBabySticker(null))

      .addEntry(TrainingEntry.forText("8kx2"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10klx2"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10cx1").peakDay(), StickerExpectations.whiteBabySticker(StickerText.P))
      .addEntry(TrainingEntry.forText("2x2"), StickerExpectations.greenBabySticker(StickerText.ONE))
      .addEntry(TrainingEntry.forText("4x1"), StickerExpectations.greenBabySticker(StickerText.TWO))
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenBabySticker(StickerText.THREE))
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker());

  public static TrainingCycle BASIC_B1F = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now()))
      .addEntry(TrainingEntry.forText("M"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("H"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("H"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("L0AD"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("4x1"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("8kx2").peakDay(), StickerExpectations.whiteBabySticker(StickerText.P))
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.whiteBabySticker(StickerText.ONE))
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenBabySticker(StickerText.TWO))

      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenBabySticker(StickerText.THREE))
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("vl10kx2").unusualBleeding(), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("vl0ad").unusualBleeding(), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenBabySticker(StickerText.ONE))
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenBabySticker(StickerText.TWO))

      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenBabySticker(StickerText.THREE))
      .addEntry(TrainingEntry.forText("2x1"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("4x2"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("4x1"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("2ad"), StickerExpectations.greenSticker());

  public static TrainingCycle BASIC_B7A = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now()))
      .addEntry(TrainingEntry.forText("L2AD"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("H"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("H"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("L2AD"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("2AD").intercourse(), StickerExpectations.greenSticker().withIntercourse())
      .addEntry(TrainingEntry.forText("2AD"), StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("2AD").intercourse(), StickerExpectations.greenSticker().withIntercourse())
      .addEntry(TrainingEntry.forText("2AD").intercourse(), StickerExpectations.greenSticker().withIntercourse())
      .addEntry(TrainingEntry.forText("2AD"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("8cx1"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("8cx1"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("8kad"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10cx1").peakDay(), StickerExpectations.whiteBabySticker(StickerText.P))

      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenBabySticker(StickerText.ONE))
      .addEntry(TrainingEntry.forText("2ad"), StickerExpectations.greenBabySticker(StickerText.TWO))
      .addEntry(TrainingEntry.forText("2ad"), StickerExpectations.greenBabySticker(StickerText.THREE))
      .addEntry(TrainingEntry.forText("4ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("2ad").intercourse(), StickerExpectations.greenSticker().withIntercourse())
      .addEntry(TrainingEntry.forText("2ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("10cklx1"), StickerExpectations.whiteBabySticker(null))

      .addEntry(TrainingEntry.forText("10ckad").peakDay(), StickerExpectations.whiteBabySticker(StickerText.P))
      .addEntry(TrainingEntry.forText("2ad"), StickerExpectations.greenBabySticker(StickerText.ONE))
      .addEntry(TrainingEntry.forText("2ad"), StickerExpectations.greenBabySticker(StickerText.TWO))
      .addEntry(TrainingEntry.forText("2ad"), StickerExpectations.greenBabySticker(StickerText.THREE))
      .addEntry(TrainingEntry.forText("2x3").intercourse(), StickerExpectations.greenSticker().withIntercourse())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("0ad").intercourse(), StickerExpectations.greenSticker().withIntercourse())

      .addEntry(TrainingEntry.forText("4x1").intercourse(), StickerExpectations.greenSticker().withIntercourse())
      .addEntry(TrainingEntry.forText("2ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("2ad").intercourse(), StickerExpectations.greenSticker().withIntercourse())
      .addEntry(TrainingEntry.forText("2ad"), StickerExpectations.greenSticker());

  public static TrainingCycle POST_PEAK_YELLOW_STAMPS = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now()))
      .addEntry(TrainingEntry.forText("M"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("H"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("L2ad"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("VL0ad"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("0ad").intercourse(), StickerExpectations.greenSticker().withIntercourse())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("2ad").intercourse(), StickerExpectations.greenSticker().withIntercourse())

      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("8cx1"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10cgx1"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10cklx2"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10klx2"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10cklad"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("4x2"), StickerExpectations.greenBabySticker(StickerText.ONE))

      .addEntry(TrainingEntry.forText("10cx1").peakDay(), StickerExpectations.whiteBabySticker(StickerText.P))
      .addEntry(TrainingEntry.forText("8cx2"), StickerExpectations.yellowBabySticker(StickerText.ONE))
      .addEntry(TrainingEntry.forText("8cgx2"), StickerExpectations.yellowBabySticker(StickerText.TWO))
      .addEntry(TrainingEntry.forText("8gyx1"), StickerExpectations.yellowBabySticker(StickerText.THREE))
      .addEntry(TrainingEntry.forText("8cx1"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.yellowSticker())

      .addEntry(TrainingEntry.forText("8cx2"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cx2"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("4ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("8cx1"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cx1"), StickerExpectations.yellowSticker());

  // From "Use of Pre-Peak Yellow Stamps in Regular Cycles (21 to 38 Days)
  // with a Mucus Cycle more thn 8 Days"
  public static TrainingCycle PRE_PEAK_YELLOW_STAMPS = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now())
          .addInstructions(BasicInstruction.K_1))
      .addEntry(TrainingEntry.forText("L2ad"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("M"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("L6cx1"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("L6cx2"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("VL6cx2"), StickerExpectations.redSticker())
      .addEntry(TrainingEntry.forText("8cx1"), StickerExpectations.yellowSticker())

      .addEntry(TrainingEntry.forText("8cx2"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cx1"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx2"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8kx2").pointOfChange(), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10kx3"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10klx1"), StickerExpectations.whiteBabySticker(null))

      .addEntry(TrainingEntry.forText("10klx2"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10wlad"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("10wlad").peakDay(), StickerExpectations.whiteBabySticker(StickerText.P))
      .addEntry(TrainingEntry.forText("2ad"), StickerExpectations.greenBabySticker(StickerText.ONE))
      .addEntry(TrainingEntry.forText("2ad"), StickerExpectations.greenBabySticker(StickerText.TWO))
      .addEntry(TrainingEntry.forText("4x1"), StickerExpectations.greenBabySticker(StickerText.THREE))
      .addEntry(TrainingEntry.forText("2ad"), StickerExpectations.greenSticker())

      .addEntry(TrainingEntry.forText("2ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("2ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("2ad"), StickerExpectations.greenSticker());

  // From Book 2, figure 6-3: The use of pre-Peak yellow stamps in a women who is breastfeeding
  public static TrainingCycle BREASTFEEDING_PRE_PEAK_YELLOW_STAMPS = TrainingCycle
      .withInstructions(Instructions.createBasicInstructions(LocalDate.now())
          .addInstructions(BasicInstruction.K_1)
          .addInstructions(
              YellowStampInstruction.YS_1_A, YellowStampInstruction.YS_1_B,
              YellowStampInstruction.YS_1_C, YellowStampInstruction.YS_1_D))
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx2"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6gcx1"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8gyx2"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx2"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.yellowSticker())

      // Starting at 8
      .addEntry(TrainingEntry.forText("8cx2"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cad"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cx2"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenSticker())
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx2"), StickerExpectations.yellowSticker())

      // Starting at 15
      .addEntry(TrainingEntry.forText("8gyx2"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8kx2").pointOfChange(), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("6cx1").pointOfChange(), StickerExpectations.yellowBabySticker(StickerText.ONE))
      .addEntry(TrainingEntry.forText("8cx2"), StickerExpectations.yellowBabySticker(StickerText.TWO))
      .addEntry(TrainingEntry.forText("8cx1"), StickerExpectations.yellowBabySticker(StickerText.THREE))
      .addEntry(TrainingEntry.forText("8cx2"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cx1"), StickerExpectations.yellowSticker())

      // Starting at 22
      .addEntry(TrainingEntry.forText("6cx2"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("10kad").pointOfChange(), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("6cx2").pointOfChange(), StickerExpectations.yellowBabySticker(StickerText.ONE))
      .addEntry(TrainingEntry.forText("0ad"), StickerExpectations.greenBabySticker(StickerText.TWO))
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.yellowBabySticker(StickerText.THREE))
      .addEntry(TrainingEntry.forText("8cx2"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cx1"), StickerExpectations.yellowSticker())

      // Starting at 29
      .addEntry(TrainingEntry.forText("8cx2"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cx1"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx2"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cgx1"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx2"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cx1"), StickerExpectations.yellowSticker())

      // Starting at 36
      .addEntry(TrainingEntry.forText("8cx2"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx2"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cad").pointOfChange(), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("8kx2"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("8kx1"), StickerExpectations.whiteBabySticker(null))
      .addEntry(TrainingEntry.forText("6cx2").pointOfChange(), StickerExpectations.yellowBabySticker(StickerText.ONE))

      // Starting at 43
      .addEntry(TrainingEntry.forText("6cx2"), StickerExpectations.yellowBabySticker(StickerText.TWO))
      .addEntry(TrainingEntry.forText("8cx2"), StickerExpectations.yellowBabySticker(StickerText.THREE))
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("6cx1"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cx2"), StickerExpectations.yellowSticker())
      .addEntry(TrainingEntry.forText("8cx2"), StickerExpectations.yellowSticker());

  public static List<TrainingCycle> basicTrainingCycles() {
    return ImmutableList.of(BASIC_B1A, BASIC_B1B, BASIC_B1C, BASIC_B1D, BASIC_B1E, BASIC_B1F, BASIC_B7A);
  }

  public static List<TrainingCycle> forRepos() {
    return basicTrainingCycles();
  }
}

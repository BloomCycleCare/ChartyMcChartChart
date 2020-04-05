package com.bloomcyclecare.cmcc.renderer;

import com.bloomcyclecare.cmcc.data.domain.BasicInstruction;
import com.bloomcyclecare.cmcc.data.domain.YellowStampInstruction;
import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.bloomcyclecare.cmcc.data.models.TrainingCycle;
import com.bloomcyclecare.cmcc.data.models.TrainingEntry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;

public class PrePeakYellowStampRendererTest extends BaseRendererTest {

  // From "Use of Pre-Peak Yellow Stamps in Regular Cycles (21 to 38 Days)
  // with a Mucus Cycle more thn 8 Days"

  @Test
  public void testYellowPrePeakExample() throws Exception {
    ImmutableMap.Builder<TrainingEntry, TrainingCycle.StickerExpectations> entries = ImmutableMap.builder();
    entries.put(TrainingEntry.forText("L2ad"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("L6cx1"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("L6cx2"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("VL6cx2"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.yellowSticker());

    entries.put(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("8kx2").pointOfChange(), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("10kx3"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("10klx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());

    entries.put(TrainingEntry.forText("10klx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("10wlad"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("10wlad").peakDay(), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("P"));
    entries.put(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("1"));
    entries.put(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(TrainingEntry.forText("4x1"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("3"));
    entries.put(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker());

    entries.put(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker());
    entries.put(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker());
    entries.put(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker());

    Instructions instructions = createInstructions(
        ImmutableList.<BasicInstruction>builder().addAll(BASIC_INSTRUCTIONS.activeItems).add(BasicInstruction.K_1).build(), ImmutableList.of(), ImmutableList.of());
    runTest(entries.build(), instructions);
  }


  // From Book 2, figure 6-3: The use of pre-Peak yellow stamps in a women who is breastfeeding

  @Test
  public void testYellowPrePeakBreastFeeding() throws Exception {
    ImmutableMap.Builder<TrainingEntry, TrainingCycle.StickerExpectations> entries = ImmutableMap.builder();
    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6gcx1"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("8gyx2"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker());

    // Starting at 8
    entries.put(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("8cad"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker());
    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.yellowSticker());

    // Starting at 15
    entries.put(TrainingEntry.forText("8gyx2"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("8kx2").pointOfChange(), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("6cx1").pointOfChange(), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("1"));
    entries.put(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("2"));
    entries.put(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("3"));
    entries.put(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.yellowSticker());

    // Starting at 22
    entries.put(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("10kad").pointOfChange(), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("6cx2").pointOfChange(), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("1"));
    entries.put(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("3"));
    entries.put(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.yellowSticker());

    // Starting at 29
    entries.put(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6cgx1"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.yellowSticker());

    // Starting at 36
    entries.put(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("8cad").pointOfChange(), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("8kx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("8kx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("6cx2").pointOfChange(), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("1"));

    // Starting at 43
    entries.put(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("2"));
    entries.put(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("3"));
    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker());

    // TODO: populate days 49-rest

    Instructions instructions = createInstructions(
        ImmutableList.<BasicInstruction>builder().addAll(BASIC_INSTRUCTIONS.activeItems).add(BasicInstruction.K_1).build(), ImmutableList.of(), ImmutableList.of(
            YellowStampInstruction.YS_1_A, YellowStampInstruction.YS_1_B, YellowStampInstruction.YS_1_C, YellowStampInstruction.YS_1_D));
    runTest(entries.build(), instructions);
  }



}

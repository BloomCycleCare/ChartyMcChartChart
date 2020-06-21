package com.bloomcyclecare.cmcc.renderer;

import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.bloomcyclecare.cmcc.models.training.TrainingCycle;
import com.bloomcyclecare.cmcc.models.training.TrainingEntry;
import com.bloomcyclecare.cmcc.models.instructions.YellowStampInstruction;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;

public class YellowStampLongCycleRendererTest extends BaseRendererTest {
  @Test
  public void testYellowLongCycle() throws Exception {
    ImmutableMap.Builder<TrainingEntry, TrainingCycle.StickerExpectations> entries = ImmutableMap.builder();
    // 1-7
    entries.put(TrainingEntry.forText("6GCBAD"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("VL6BAD"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("L6BGX3").intercourse(), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("L6BGX1").intercourse(), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("VL0AD").intercourse(), TrainingCycle.StickerExpectations.redSticker());

    // 8-14
    entries.put(TrainingEntry.forText("6cgx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("6cpgx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("6cgx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("6cpx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("6cpx3"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("6cgpx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("6cgpx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());

    // 15-21
    entries.put(TrainingEntry.forText("6cgx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("6cx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("6cgx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("10ckx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("8ckx1").pointOfChange(), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("1"));

    // 22-28
    entries.put(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(TrainingEntry.forText("6cgx1"), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("3"));
    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6cgx1"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("8ckx1").pointOfChange(), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    // TODO: check with becky that this should be yellow baby
    // TODO: is POC always a N for ESQ?
    // TODO:
    entries.put(TrainingEntry.forText("6cgx1"), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("1"));
    entries.put(TrainingEntry.forText("10kx1").pointOfChange(), TrainingCycle.StickerExpectations.whiteSticker().withBaby());

    // 29-35
    entries.put(TrainingEntry.forText("8ckx1"), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("1"));
    entries.put(TrainingEntry.forText("0ad").pointOfChange(), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(TrainingEntry.forText("6cad"), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("3"));
    Instructions longCycleInstructions =
        createInstructions(ImmutableList.of(), ImmutableList.of(), ImmutableList.of(
            YellowStampInstruction.YS_1_A, YellowStampInstruction.YS_1_B, YellowStampInstruction.YS_1_C, YellowStampInstruction.YS_1_D,
            YellowStampInstruction.YS_2_A, YellowStampInstruction.YS_2_B, YellowStampInstruction.YS_3_B));

    //runTest(entries.build(), BASIC_INSTRUCTIONS, longCycleInstructions);
  }
}

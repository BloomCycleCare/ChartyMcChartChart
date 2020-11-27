package com.bloomcyclecare.cmcc.renderer;

import com.bloomcyclecare.cmcc.data.models.instructions.Instructions;
import com.bloomcyclecare.cmcc.data.models.instructions.YellowStampInstruction;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerText;
import com.bloomcyclecare.cmcc.data.models.training.StickerExpectations;
import com.bloomcyclecare.cmcc.data.models.training.TrainingEntry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;

public class YellowStampLongCycleRendererTest extends BaseRendererTest {
  @Test
  public void testYellowLongCycle() throws Exception {
    ImmutableMap.Builder<TrainingEntry, StickerExpectations> entries = ImmutableMap.builder();
    // 1-7
    entries.put(TrainingEntry.forText("6GCBAD"), StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("VL6BAD"), StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("M"), StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("M"), StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("L6BGX3").intercourse(), StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("L6BGX1").intercourse(), StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("VL0AD").intercourse(), StickerExpectations.redSticker());

    // 8-14
    entries.put(TrainingEntry.forText("6cgx1"), StickerExpectations.whiteBabySticker(null));
    entries.put(TrainingEntry.forText("6cpgx2"), StickerExpectations.whiteBabySticker(null));
    entries.put(TrainingEntry.forText("6cgx2"), StickerExpectations.whiteBabySticker(null));
    entries.put(TrainingEntry.forText("6cpx1"), StickerExpectations.whiteBabySticker(null));
    entries.put(TrainingEntry.forText("6cpx3"), StickerExpectations.whiteBabySticker(null));
    entries.put(TrainingEntry.forText("6cgpx1"), StickerExpectations.whiteBabySticker(null));
    entries.put(TrainingEntry.forText("6cgpx2"), StickerExpectations.whiteBabySticker(null));

    // 15-21
    entries.put(TrainingEntry.forText("6cgx1"), StickerExpectations.whiteBabySticker(null));
    entries.put(TrainingEntry.forText("6cx2"), StickerExpectations.whiteBabySticker(null));
    entries.put(TrainingEntry.forText("6cx1"), StickerExpectations.whiteBabySticker(null));
    entries.put(TrainingEntry.forText("6cgx2"), StickerExpectations.whiteBabySticker(null));
    entries.put(TrainingEntry.forText("10ckx1"), StickerExpectations.whiteBabySticker(null));
    entries.put(TrainingEntry.forText("8ckx1").pointOfChange(), StickerExpectations.whiteBabySticker(null));
    entries.put(TrainingEntry.forText("0ad"), StickerExpectations.greenBabySticker(StickerText.ONE));

    // 22-28
    entries.put(TrainingEntry.forText("0ad"), StickerExpectations.greenBabySticker(StickerText.TWO));
    entries.put(TrainingEntry.forText("6cgx1"), StickerExpectations.yellowBabySticker(StickerText.THREE));
    entries.put(TrainingEntry.forText("6cx1"), StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6cgx1"), StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("8ckx1").pointOfChange(), StickerExpectations.whiteBabySticker(null));
    // TODO: check with becky that this should be yellow baby
    // TODO: is POC always a N for ESQ?
    // TODO:
    entries.put(TrainingEntry.forText("6cgx1"), StickerExpectations.yellowBabySticker(StickerText.ONE));
    entries.put(TrainingEntry.forText("10kx1").pointOfChange(), StickerExpectations.whiteBabySticker(null));

    // 29-35
    entries.put(TrainingEntry.forText("8ckx1"), StickerExpectations.yellowBabySticker(StickerText.ONE));
    entries.put(TrainingEntry.forText("0ad").pointOfChange(), StickerExpectations.greenBabySticker(StickerText.TWO));
    entries.put(TrainingEntry.forText("6cad"), StickerExpectations.yellowBabySticker(StickerText.THREE));
    Instructions longCycleInstructions =
        createInstructions(ImmutableList.of(), ImmutableList.of(), ImmutableList.of(
            YellowStampInstruction.YS_1_A, YellowStampInstruction.YS_1_B, YellowStampInstruction.YS_1_C, YellowStampInstruction.YS_1_D,
            YellowStampInstruction.YS_2_A, YellowStampInstruction.YS_2_B, YellowStampInstruction.YS_3_B));

    //runTest(entries.build(), BASIC_INSTRUCTIONS, longCycleInstructions);
  }
}

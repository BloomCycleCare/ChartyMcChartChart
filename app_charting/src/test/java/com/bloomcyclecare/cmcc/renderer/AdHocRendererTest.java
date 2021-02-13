package com.bloomcyclecare.cmcc.renderer;

import com.bloomcyclecare.cmcc.data.models.stickering.StickerText;
import com.bloomcyclecare.cmcc.data.models.training.StickerExpectations;
import com.bloomcyclecare.cmcc.data.models.training.TrainingCycle;
import com.bloomcyclecare.cmcc.data.models.training.TrainingEntry;

import org.junit.Test;

public class AdHocRendererTest extends BaseRendererTest {

  @Test
  public void test1() throws Exception {
    TrainingCycle cycle = TrainingCycle.withInstructions(BASIC_INSTRUCTIONS)
        .addEntry(TrainingEntry.forText("H"), StickerExpectations.redSticker())
        .addEntry(TrainingEntry.forText("M"), StickerExpectations.redSticker())
        .addEntry(TrainingEntry.forText("L0AD"), StickerExpectations.redSticker())
        .addEntry(TrainingEntry.forText("8CAD"), StickerExpectations.whiteBabySticker(null))
        .addEntry(TrainingEntry.forText("6CAD"), StickerExpectations.whiteBabySticker(null))
        .addEntry(TrainingEntry.forText("6CAD"), StickerExpectations.whiteBabySticker(null))
        .addEntry(TrainingEntry.forText("6CKAD"), StickerExpectations.whiteBabySticker(null))
        .addEntry(TrainingEntry.forText("6CAD"), StickerExpectations.whiteBabySticker(StickerText.ONE))
        .addEntry(TrainingEntry.forText("6CAD"), StickerExpectations.whiteBabySticker(StickerText.TWO))
        .addEntry(TrainingEntry.forText("0AD"), StickerExpectations.greenBabySticker(StickerText.THREE))
        .addEntry(TrainingEntry.forText("0AD"), StickerExpectations.greenSticker())
        .addEntry(TrainingEntry.forText("0AD"), StickerExpectations.greenSticker())
        .addEntry(TrainingEntry.forText("0AD"), StickerExpectations.greenSticker())
        ;
    runTest(cycle);
  }
}

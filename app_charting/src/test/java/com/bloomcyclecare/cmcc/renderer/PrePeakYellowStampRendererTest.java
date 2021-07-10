package com.bloomcyclecare.cmcc.renderer;

import com.bloomcyclecare.cmcc.data.models.charting.DemoCycles;
import com.bloomcyclecare.cmcc.data.models.instructions.BasicInstruction;
import com.bloomcyclecare.cmcc.data.models.instructions.YellowStampInstruction;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerText;
import com.bloomcyclecare.cmcc.data.models.training.StickerExpectations;
import com.bloomcyclecare.cmcc.data.models.training.TrainingEntry;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;

import java.util.Optional;

public class PrePeakYellowStampRendererTest extends BaseRendererTest {

  @Test
  public void testYellowPrePeakExample() throws Exception {
    runTest(DemoCycles.PRE_PEAK_YELLOW_STAMPS);
  }

  @Test
  public void testYellowPrePeakBreastFeeding() throws Exception {
    runTest(DemoCycles.BREASTFEEDING_PRE_PEAK_YELLOW_STAMPS);
  }

  @Test
  public void testUncertainCount() throws Exception {
    ImmutableMap.Builder<TrainingEntry, Optional<StickerExpectations>> entries = ImmutableMap.builder();
    entries.put(TrainingEntry.forText("M"), Optional.of(StickerExpectations.redSticker()));
    entries.put(TrainingEntry.forText("H"), Optional.of(StickerExpectations.redSticker()));
    entries.put(TrainingEntry.forText("H"), Optional.of(StickerExpectations.redSticker()));
    entries.put(TrainingEntry.forText("M"), Optional.of(StickerExpectations.redSticker()));
    entries.put(TrainingEntry.forText("L0AD").intercourse(), Optional.of(StickerExpectations.redSticker()));
    entries.put(TrainingEntry.forText("VL0AD"), Optional.of(StickerExpectations.redSticker()));
    entries.put(TrainingEntry.forText("0AD").uncertain(), Optional.of(StickerExpectations.greenBabySticker(null)));

    entries.put(TrainingEntry.forText("0AD"), Optional.of(StickerExpectations.greenBabySticker(StickerText.ONE)));
    entries.put(TrainingEntry.forText("6CX1"), Optional.of(StickerExpectations.yellowBabySticker(StickerText.TWO)));
    entries.put(TrainingEntry.forText("8CX1").pointOfChange(), Optional.of(StickerExpectations.whiteBabySticker(null)));
    entries.put(TrainingEntry.forText("8CX1"), Optional.of(StickerExpectations.whiteBabySticker(null)));
    entries.put(TrainingEntry.forText("8CX1"), Optional.of(StickerExpectations.whiteBabySticker(null)));
    entries.put(TrainingEntry.forText("8KX2"), Optional.of(StickerExpectations.whiteBabySticker(null)));
    entries.put(TrainingEntry.forText("10CX2"), Optional.of(StickerExpectations.whiteBabySticker(null)));

    runTest(entries.build(), BASIC_INSTRUCTIONS
        .copyOf()
        .addInstructions(BasicInstruction.K_1)
        .addInstructions(YellowStampInstruction.YS_1_C));
  }
}

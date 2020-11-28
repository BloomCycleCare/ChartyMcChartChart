package com.bloomcyclecare.cmcc.renderer;

import com.bloomcyclecare.cmcc.data.models.instructions.SpecialInstruction;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerText;
import com.bloomcyclecare.cmcc.data.models.training.StickerExpectations;
import com.bloomcyclecare.cmcc.data.models.training.TrainingEntry;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;

import java.util.Optional;

public class SpecialInstructionRendererTest extends BaseRendererTest {
  @Test
  public void testBreastFeedingSeminalFluidInstruction() throws Exception {
    ImmutableMap.Builder<TrainingEntry, Optional<StickerExpectations>> entries = ImmutableMap.builder();
    entries.put(TrainingEntry.forText("0AD"), Optional.of(StickerExpectations.greenSticker()));
    entries.put(TrainingEntry.forText("0AD"), Optional.of(StickerExpectations.greenSticker()));

    entries.put(TrainingEntry.forText("0AD").intercourse(), Optional.of(StickerExpectations.greenSticker()));
    entries.put(TrainingEntry.forText("10Cx1").essentiallyTheSame(), Optional.of(StickerExpectations.yellowSticker()));
    entries.put(TrainingEntry.forText("2AD"), Optional.of(StickerExpectations.greenSticker()));

    entries.put(TrainingEntry.forText("0AD").intercourse(), Optional.of(StickerExpectations.greenSticker()));
    entries.put(TrainingEntry.forText("10Cx1"), Optional.of(StickerExpectations.whiteBabySticker(null)));
    entries.put(TrainingEntry.forText("0AD"), Optional.of(StickerExpectations.greenBabySticker(StickerText.ONE)));
    entries.put(TrainingEntry.forText("0AD"), Optional.of(StickerExpectations.greenBabySticker(StickerText.TWO)));
    entries.put(TrainingEntry.forText("0AD"), Optional.of(StickerExpectations.greenBabySticker(StickerText.THREE)));
    entries.put(TrainingEntry.forText("0AD"), Optional.of(StickerExpectations.greenSticker()));

    runTest(entries.build(), BASIC_INSTRUCTIONS.copyOf().addInstructions(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS));
  }
}

package com.bloomcyclecare.cmcc.renderer;

import com.bloomcyclecare.cmcc.data.domain.BasicInstruction;
import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.bloomcyclecare.cmcc.data.models.TrainingCycle;
import com.bloomcyclecare.cmcc.data.models.TrainingEntry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;

public class PostPeakYellowStampRendererTest extends BaseRendererTest {

  @Test
  public void testYellowPostPeakK2() throws Exception {
    runPostPeakTest(BasicInstruction.K_2);
  }

  @Test
  public void testYellowPostPeakK3() throws Exception {
    runPostPeakTest(BasicInstruction.K_3);
  }

  @Test
  public void testYellowPostPeakK4() throws Exception {
    runPostPeakTest(BasicInstruction.K_4);
  }

  private void runPostPeakTest(BasicInstruction postPeakInstruction) throws Exception {
    ImmutableMap.Builder<TrainingEntry, TrainingCycle.StickerExpectations> entries = ImmutableMap.builder();
    entries.put(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("L2ad"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("VL0ad"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("0ad").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse());
    entries.put(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker());
    entries.put(TrainingEntry.forText("2ad").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse());

    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("10cgx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("10cklx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("10klx2"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("10cklad"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("4x2"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("1"));

    entries.put(TrainingEntry.forText("10cx1").peakDay(), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("P"));
    entries.put(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("1"));
    entries.put(TrainingEntry.forText("8cgx2"), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("2"));
    entries.put(TrainingEntry.forText("8gyx1"), TrainingCycle.StickerExpectations.yellowSticker().withBaby().withPeakText("3"));
    entries.put(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker());

    entries.put(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("8cx2"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("4ad"), TrainingCycle.StickerExpectations.greenSticker());
    entries.put(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker());
    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.yellowSticker());
    entries.put(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.yellowSticker());
    Instructions instructions =
        createInstructions(ImmutableList.<BasicInstruction>builder()
            .addAll(BASIC_INSTRUCTIONS.activeItems)
            .add(postPeakInstruction)
            .build(), ImmutableList.of(), ImmutableList.of());
    runTest(entries.build(), instructions);
  }

}

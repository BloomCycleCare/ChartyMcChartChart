package com.bloomcyclecare.cmcc.renderer;

import android.content.Context;

import com.bloomcyclecare.cmcc.data.models.DemoCycles;
import com.bloomcyclecare.cmcc.data.models.TrainingCycle;
import com.bloomcyclecare.cmcc.data.models.TrainingEntry;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Created by parkeroth on 7/1/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class BasicInstructionRendererTest extends BaseRendererTest {

  @Mock
  Context mContext;

  @Test
  public void testB1A() throws Exception {
    runTest(DemoCycles.BASIC_B1A);
  }

  @Test
  public void testB1B() throws Exception {
    runTest(DemoCycles.BASIC_B1B);
  }

  @Test
  public void testB1C() throws Exception {
    runTest(DemoCycles.BASIC_B1C);
  }

  @Test
  public void testB1D() throws Exception {
    runTest(DemoCycles.BASIC_B1D);
  }

  @Test
  public void testB1E() throws Exception {
    runTest(DemoCycles.BASIC_B1E);
  }

  @Test
  public void testB1F() throws Exception {
    runTest(DemoCycles.BASIC_B1F);
  }

  // TODO: B2

  @Test
  public void testB7A() throws Exception {
    ImmutableMap.Builder<TrainingEntry, TrainingCycle.StickerExpectations> entries = ImmutableMap.builder();
    entries.put(TrainingEntry.forText("L2AD"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("L2AD"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("2AD").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse());
    entries.put(TrainingEntry.forText("2AD"), TrainingCycle.StickerExpectations.greenSticker());

    entries.put(TrainingEntry.forText("2AD").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse());
    entries.put(TrainingEntry.forText("2AD").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse());
    entries.put(TrainingEntry.forText("2AD"), TrainingCycle.StickerExpectations.greenSticker());
    entries.put(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("8cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("8kad"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("10cx1").peakDay(), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("P"));

    entries.put(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("1"));
    entries.put(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("3"));
    entries.put(TrainingEntry.forText("4ad"), TrainingCycle.StickerExpectations.greenSticker());
    entries.put(TrainingEntry.forText("2ad").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse());
    entries.put(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker());
    entries.put(TrainingEntry.forText("10cklx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());

    entries.put(TrainingEntry.forText("10ckad").peakDay(), TrainingCycle.StickerExpectations.whiteSticker().withBaby().withPeakText("P"));
    entries.put(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("1"));
    entries.put(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker().withBaby().withPeakText("3"));
    entries.put(TrainingEntry.forText("2x3").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse());
    entries.put(TrainingEntry.forText("0ad"), TrainingCycle.StickerExpectations.greenSticker());
    entries.put(TrainingEntry.forText("0ad").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse());

    entries.put(TrainingEntry.forText("4x1").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse());
    entries.put(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker());
    entries.put(TrainingEntry.forText("2ad").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse());
    entries.put(TrainingEntry.forText("2ad"), TrainingCycle.StickerExpectations.greenSticker());
    runTest(entries.build(), BASIC_INSTRUCTIONS);
  }

  // TODO: B7B - "Missed Period" form of "Double Peak"

  @Test
  public void testNoBabiesOrNumbersIfEmpty() throws Exception {
    ImmutableMap.Builder<TrainingEntry, TrainingCycle.StickerExpectations> entries = ImmutableMap.builder();
    entries.put(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("L2AD"), TrainingCycle.StickerExpectations.redSticker());
    entries.put(TrainingEntry.forText("2AD").intercourse(), TrainingCycle.StickerExpectations.greenSticker().withIntercourse());
    entries.put(TrainingEntry.forText("2AD"), TrainingCycle.StickerExpectations.greenSticker());

    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText("6cx1"), TrainingCycle.StickerExpectations.whiteSticker().withBaby());
    entries.put(TrainingEntry.forText(""), TrainingCycle.StickerExpectations.greySticker());
    entries.put(TrainingEntry.forText(""), TrainingCycle.StickerExpectations.greySticker());
    entries.put(TrainingEntry.forText(""), TrainingCycle.StickerExpectations.greySticker());
    entries.put(TrainingEntry.forText(""), TrainingCycle.StickerExpectations.greySticker());
    runTest(entries.build(), BASIC_INSTRUCTIONS);
  }
}
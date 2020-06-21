package com.bloomcyclecare.cmcc.renderer;

import android.content.Context;

import com.bloomcyclecare.cmcc.models.charting.DemoCycles;
import com.bloomcyclecare.cmcc.models.training.TrainingCycle;
import com.bloomcyclecare.cmcc.models.training.TrainingEntry;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

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
    runTest(DemoCycles.BASIC_B7A);
  }

  // TODO: B7B - "Missed Period" form of "Double Peak"

  @Test
  public void testNoBabiesOrNumbersIfEmpty() throws Exception {
    ImmutableMap.Builder<TrainingEntry, Optional<TrainingCycle.StickerExpectations>> entries = ImmutableMap.builder();
    entries.put(TrainingEntry.forText("H"), Optional.of(TrainingCycle.StickerExpectations.redSticker()));
    entries.put(TrainingEntry.forText("H"), Optional.of(TrainingCycle.StickerExpectations.redSticker()));
    entries.put(TrainingEntry.forText("M"), Optional.of(TrainingCycle.StickerExpectations.redSticker()));
    entries.put(TrainingEntry.forText("M"), Optional.of(TrainingCycle.StickerExpectations.redSticker()));
    entries.put(TrainingEntry.forText("L2AD"), Optional.of(TrainingCycle.StickerExpectations.redSticker()));
    entries.put(TrainingEntry.forText("2AD").intercourse(), Optional.of(TrainingCycle.StickerExpectations.greenSticker().withIntercourse()));
    entries.put(TrainingEntry.forText("2AD"), Optional.of(TrainingCycle.StickerExpectations.greenSticker()));

    entries.put(TrainingEntry.forText("6cx1"), Optional.of(TrainingCycle.StickerExpectations.whiteSticker().withBaby()));
    entries.put(TrainingEntry.forText("6cx1"), Optional.of(TrainingCycle.StickerExpectations.whiteSticker().withBaby()));
    entries.put(TrainingEntry.forText("6cx1"), Optional.of(TrainingCycle.StickerExpectations.whiteSticker().withBaby()));
    entries.put(TrainingEntry.forText(""), Optional.of(TrainingCycle.StickerExpectations.greySticker()));
    entries.put(TrainingEntry.forText(""), Optional.of(TrainingCycle.StickerExpectations.greySticker()));
    entries.put(TrainingEntry.forText(""), Optional.of(TrainingCycle.StickerExpectations.greySticker()));
    entries.put(TrainingEntry.forText(""), Optional.of(TrainingCycle.StickerExpectations.greySticker()));
    runTest(entries.build(), BASIC_INSTRUCTIONS);
  }

  @Test
  public void testFoo() throws Exception {
    ImmutableMap.Builder<TrainingEntry, Optional<TrainingCycle.StickerExpectations>> entries = ImmutableMap.builder();
    entries.put(TrainingEntry.forText(""), Optional.of(TrainingCycle.StickerExpectations.greySticker()));
    entries.put(TrainingEntry.forText(""), Optional.of(TrainingCycle.StickerExpectations.greySticker()));
    entries.put(TrainingEntry.forText(""), Optional.of(TrainingCycle.StickerExpectations.greySticker()));
    entries.put(TrainingEntry.forText(""), Optional.of(TrainingCycle.StickerExpectations.greySticker()));
    entries.put(TrainingEntry.forText(""), Optional.of(TrainingCycle.StickerExpectations.greySticker()));
    entries.put(TrainingEntry.forText(""), Optional.of(TrainingCycle.StickerExpectations.greySticker()));
    entries.put(TrainingEntry.forText("0AD"), Optional.of(TrainingCycle.StickerExpectations.greenSticker()));

    runTest(entries.build(), BASIC_INSTRUCTIONS);
  }
}

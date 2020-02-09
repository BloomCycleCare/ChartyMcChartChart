package com.bloomcyclecare.cmcc.renderer;

import android.content.Context;

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
    ImmutableMap.Builder<Entry, Expectations> entries = ImmutableMap.builder();
    entries.put(Entry.forText("H"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("L 0AD"), Expectations.redSticker());
    entries.put(Entry.forText("2x1"), Expectations.greenSticker());
    entries.put(Entry.forText("0AD"), Expectations.greenSticker());
    entries.put(Entry.forText("0AD"), Expectations.greenSticker());

    entries.put(Entry.forText("0AD"), Expectations.greenSticker());
    entries.put(Entry.forText("6cx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("8cx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("8kx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10klAD"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10klAD").peakDay(), Expectations.whiteSticker().withBaby().withPeakText("P"));
    entries.put(Entry.forText("6cx1"), Expectations.whiteSticker().withBaby().withPeakText("1"));

    entries.put(Entry.forText("0AD"), Expectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(Entry.forText("4x1"), Expectations.greenSticker().withBaby().withPeakText("3"));
    entries.put(Entry.forText("0AD"), Expectations.greenSticker());
    entries.put(Entry.forText("0AD"), Expectations.greenSticker());
    entries.put(Entry.forText("2x1"), Expectations.greenSticker());
    runTest(entries.build(), BASIC_INSTRUCTIONS);
  }

  @Test
  public void testB1B() throws Exception {
    ImmutableMap.Builder<Entry, Expectations> entries = ImmutableMap.builder();
    entries.put(Entry.forText("H"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("L0AD"), Expectations.redSticker());
    entries.put(Entry.forText("VL2x1"), Expectations.redSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());

    entries.put(Entry.forText("6cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("6cx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("8cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());

    entries.put(Entry.forText("6cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("8cx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("8cad"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10cx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10klad"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10klad").peakDay(), Expectations.whiteSticker().withBaby().withPeakText("P"));
    entries.put(Entry.forText("8cx1"), Expectations.whiteSticker().withBaby().withPeakText("1"));

    entries.put(Entry.forText("4x1"), Expectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("3"));
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());

    runTest(entries.build(), BASIC_INSTRUCTIONS);
  }

  @Test
  public void testB1C() throws Exception {
    ImmutableMap.Builder<Entry, Expectations> entries = ImmutableMap.builder();
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("H"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("L0AD"), Expectations.redSticker());
    entries.put(Entry.forText("2x2"), Expectations.greenSticker());
    entries.put(Entry.forText("4x1"), Expectations.greenSticker());
    entries.put(Entry.forText("2ad"), Expectations.greenSticker());

    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("6cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("6cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("8cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("1"));
    entries.put(Entry.forText("2x2"), Expectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(Entry.forText("4x1"), Expectations.greenSticker().withBaby().withPeakText("3"));

    entries.put(Entry.forText("4x1"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("8cx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10kad"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10klad"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10wlx2").peakDay(), Expectations.whiteSticker().withBaby().withPeakText("P"));
    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("1"));

    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("3"));
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());

    runTest(entries.build(), BASIC_INSTRUCTIONS);
  }

  @Test
  public void testB1D() throws Exception {
    ImmutableMap.Builder<Entry, Expectations> entries = ImmutableMap.builder();
    entries.put(Entry.forText("L0AD"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("H"), Expectations.redSticker());
    entries.put(Entry.forText("L8CX1"), Expectations.redSticker());
    entries.put(Entry.forText("8cx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10kx2"), Expectations.whiteSticker().withBaby());

    entries.put(Entry.forText("10klad"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10cx1").peakDay(), Expectations.whiteSticker().withBaby().withPeakText("P"));
    entries.put(Entry.forText("6cx1"), Expectations.whiteSticker().withBaby().withPeakText("1"));
    entries.put(Entry.forText("8cx2"), Expectations.whiteSticker().withBaby().withPeakText("2"));
    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("3"));
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());

    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("4x2"), Expectations.greenSticker());
    entries.put(Entry.forText("4x1"), Expectations.greenSticker());
    entries.put(Entry.forText("4ad"), Expectations.greenSticker());
    entries.put(Entry.forText("4ad"), Expectations.greenSticker());
    entries.put(Entry.forText("2x1"), Expectations.greenSticker());

    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());

    runTest(entries.build(), BASIC_INSTRUCTIONS);
  }

  @Test
  public void testB1E() throws Exception {
    ImmutableMap.Builder<Entry, Expectations> entries = ImmutableMap.builder();
    entries.put(Entry.forText("L0AD"), Expectations.redSticker());
    entries.put(Entry.forText("H"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("L2x2"), Expectations.redSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("10cx1").peakDay(), Expectations.whiteSticker().withBaby().withPeakText("P"));

    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("1"));
    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("3"));
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("8cx1"), Expectations.whiteSticker().withBaby());

    entries.put(Entry.forText("8kx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10klx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10cx1").peakDay(), Expectations.whiteSticker().withBaby().withPeakText("P"));
    entries.put(Entry.forText("2x2"), Expectations.greenSticker().withBaby().withPeakText("1"));
    entries.put(Entry.forText("4x1"), Expectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("3"));
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());

    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());

    entries.put(Entry.forText("0ad"), Expectations.greenSticker());

    runTest(entries.build(), BASIC_INSTRUCTIONS);
  }

  @Test
  public void testB1F() throws Exception {
    ImmutableMap.Builder<Entry, Expectations> entries = ImmutableMap.builder();
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("H"), Expectations.redSticker());
    entries.put(Entry.forText("H"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("L0AD"), Expectations.redSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());

    entries.put(Entry.forText("4x1"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("6cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("8kx2").peakDay(), Expectations.whiteSticker().withBaby().withPeakText("P"));
    entries.put(Entry.forText("6cx1"), Expectations.whiteSticker().withBaby().withPeakText("1"));
    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("2"));

    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("3"));
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("vl10kx2").unusualBleeding(), Expectations.redSticker());
    entries.put(Entry.forText("vl0ad").unusualBleeding(), Expectations.redSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("1"));
    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("2"));

    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("3"));
    entries.put(Entry.forText("2x1"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("4x2"), Expectations.greenSticker());
    entries.put(Entry.forText("4x1"), Expectations.greenSticker());
    entries.put(Entry.forText("2ad"), Expectations.greenSticker());

    runTest(entries.build(), BASIC_INSTRUCTIONS);
  }

  // TODO: B2

  @Test
  public void testB7A() throws Exception {
    ImmutableMap.Builder<Entry, Expectations> entries = ImmutableMap.builder();
    entries.put(Entry.forText("L2AD"), Expectations.redSticker());
    entries.put(Entry.forText("H"), Expectations.redSticker());
    entries.put(Entry.forText("H"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("L2AD"), Expectations.redSticker());
    entries.put(Entry.forText("2AD").intercourse(), Expectations.greenSticker().withIntercourse());
    entries.put(Entry.forText("2AD"), Expectations.greenSticker());

    entries.put(Entry.forText("2AD").intercourse(), Expectations.greenSticker().withIntercourse());
    entries.put(Entry.forText("2AD").intercourse(), Expectations.greenSticker().withIntercourse());
    entries.put(Entry.forText("2AD"), Expectations.greenSticker());
    entries.put(Entry.forText("8cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("8cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("8kad"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10cx1").peakDay(), Expectations.whiteSticker().withBaby().withPeakText("P"));

    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("1"));
    entries.put(Entry.forText("2ad"), Expectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(Entry.forText("2ad"), Expectations.greenSticker().withBaby().withPeakText("3"));
    entries.put(Entry.forText("4ad"), Expectations.greenSticker());
    entries.put(Entry.forText("2ad").intercourse(), Expectations.greenSticker().withIntercourse());
    entries.put(Entry.forText("2ad"), Expectations.greenSticker());
    entries.put(Entry.forText("10cklx1"), Expectations.whiteSticker().withBaby());

    entries.put(Entry.forText("10ckad").peakDay(), Expectations.whiteSticker().withBaby().withPeakText("P"));
    entries.put(Entry.forText("2ad"), Expectations.greenSticker().withBaby().withPeakText("1"));
    entries.put(Entry.forText("2ad"), Expectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(Entry.forText("2ad"), Expectations.greenSticker().withBaby().withPeakText("3"));
    entries.put(Entry.forText("2x3").intercourse(), Expectations.greenSticker().withIntercourse());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("0ad").intercourse(), Expectations.greenSticker().withIntercourse());

    entries.put(Entry.forText("4x1").intercourse(), Expectations.greenSticker().withIntercourse());
    entries.put(Entry.forText("2ad"), Expectations.greenSticker());
    entries.put(Entry.forText("2ad").intercourse(), Expectations.greenSticker().withIntercourse());
    entries.put(Entry.forText("2ad"), Expectations.greenSticker());
    runTest(entries.build(), BASIC_INSTRUCTIONS);
  }

  // TODO: B7B - "Missed Period" form of "Double Peak"

  @Test
  public void testNoBabiesOrNumbersIfEmpty() throws Exception {
    ImmutableMap.Builder<Entry, Expectations> entries = ImmutableMap.builder();
    entries.put(Entry.forText("H"), Expectations.redSticker());
    entries.put(Entry.forText("H"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("L2AD"), Expectations.redSticker());
    entries.put(Entry.forText("2AD").intercourse(), Expectations.greenSticker().withIntercourse());
    entries.put(Entry.forText("2AD"), Expectations.greenSticker());

    entries.put(Entry.forText("6cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("6cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("6cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText(""), Expectations.greySticker());
    entries.put(Entry.forText(""), Expectations.greySticker());
    entries.put(Entry.forText(""), Expectations.greySticker());
    entries.put(Entry.forText(""), Expectations.greySticker());
    runTest(entries.build(), BASIC_INSTRUCTIONS);
  }
}

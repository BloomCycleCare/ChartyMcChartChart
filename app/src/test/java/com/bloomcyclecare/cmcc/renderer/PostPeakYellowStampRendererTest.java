package com.bloomcyclecare.cmcc.renderer;

import com.bloomcyclecare.cmcc.data.domain.BasicInstruction;
import com.bloomcyclecare.cmcc.data.entities.Instructions;
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
    ImmutableMap.Builder<BaseRendererTest.Entry, BaseRendererTest.Expectations> entries = ImmutableMap.builder();
    entries.put(BaseRendererTest.Entry.forText("M"), BaseRendererTest.Expectations.redSticker());
    entries.put(BaseRendererTest.Entry.forText("H"), BaseRendererTest.Expectations.redSticker());
    entries.put(BaseRendererTest.Entry.forText("L2ad"), BaseRendererTest.Expectations.redSticker());
    entries.put(BaseRendererTest.Entry.forText("VL0ad"), BaseRendererTest.Expectations.redSticker());
    entries.put(BaseRendererTest.Entry.forText("0ad").intercourse(), BaseRendererTest.Expectations.greenSticker().withIntercourse());
    entries.put(BaseRendererTest.Entry.forText("0ad"), BaseRendererTest.Expectations.greenSticker());
    entries.put(BaseRendererTest.Entry.forText("2ad").intercourse(), BaseRendererTest.Expectations.greenSticker().withIntercourse());

    entries.put(BaseRendererTest.Entry.forText("6cx1"), BaseRendererTest.Expectations.whiteSticker().withBaby());
    entries.put(BaseRendererTest.Entry.forText("8cx1"), BaseRendererTest.Expectations.whiteSticker().withBaby());
    entries.put(BaseRendererTest.Entry.forText("10cgx1"), BaseRendererTest.Expectations.whiteSticker().withBaby());
    entries.put(BaseRendererTest.Entry.forText("10cklx2"), BaseRendererTest.Expectations.whiteSticker().withBaby());
    entries.put(BaseRendererTest.Entry.forText("10klx2"), BaseRendererTest.Expectations.whiteSticker().withBaby());
    entries.put(BaseRendererTest.Entry.forText("10cklad"), BaseRendererTest.Expectations.whiteSticker().withBaby());
    entries.put(BaseRendererTest.Entry.forText("4x2"), BaseRendererTest.Expectations.greenSticker().withBaby().withPeakText("1"));

    entries.put(BaseRendererTest.Entry.forText("10cx1").peakDay(), BaseRendererTest.Expectations.whiteSticker().withBaby().withPeakText("P"));
    entries.put(BaseRendererTest.Entry.forText("8cx2"), BaseRendererTest.Expectations.yellowSticker().withBaby().withPeakText("1"));
    entries.put(BaseRendererTest.Entry.forText("8cgx2"), BaseRendererTest.Expectations.yellowSticker().withBaby().withPeakText("2"));
    entries.put(BaseRendererTest.Entry.forText("8gyx1"), BaseRendererTest.Expectations.yellowSticker().withBaby().withPeakText("3"));
    entries.put(BaseRendererTest.Entry.forText("8cx1"), BaseRendererTest.Expectations.yellowSticker());
    entries.put(BaseRendererTest.Entry.forText("6cx1"), BaseRendererTest.Expectations.yellowSticker());
    entries.put(BaseRendererTest.Entry.forText("6cx1"), BaseRendererTest.Expectations.yellowSticker());

    entries.put(BaseRendererTest.Entry.forText("8cx2"), BaseRendererTest.Expectations.yellowSticker());
    entries.put(BaseRendererTest.Entry.forText("8cx2"), BaseRendererTest.Expectations.yellowSticker());
    entries.put(BaseRendererTest.Entry.forText("4ad"), BaseRendererTest.Expectations.greenSticker());
    entries.put(BaseRendererTest.Entry.forText("8cx1"), BaseRendererTest.Expectations.yellowSticker());
    entries.put(BaseRendererTest.Entry.forText("0ad"), BaseRendererTest.Expectations.greenSticker());
    entries.put(BaseRendererTest.Entry.forText("6cx1"), BaseRendererTest.Expectations.yellowSticker());
    entries.put(BaseRendererTest.Entry.forText("8cx1"), BaseRendererTest.Expectations.yellowSticker());
    Instructions instructions =
        createInstructions(ImmutableList.<BasicInstruction>builder()
            .addAll(BASIC_INSTRUCTIONS.activeItems)
            .add(postPeakInstruction)
            .build(), ImmutableList.of(), ImmutableList.of());
    runTest(entries.build(), instructions);
  }

}

package com.roamingroths.cmcc.renderer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.roamingroths.cmcc.data.domain.BasicInstruction;
import com.roamingroths.cmcc.data.entities.Instructions;

import org.junit.Test;

public class PostPeakYellowStampRendererTest extends BaseRendererTest {

  @Test
  public void testYellowPostPeakExample() throws Exception {
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
        createInstructions(ImmutableList.<BasicInstruction>builder().addAll(BASIC_INSTRUCTIONS.activeItems).add(BasicInstruction.K_2).build(), ImmutableList.of(), ImmutableList.of());
    runTest(entries.build(), instructions);
  }

}

package com.roamingroths.cmcc.renderer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.roamingroths.cmcc.data.domain.BasicInstruction;
import com.roamingroths.cmcc.data.entities.Instructions;

import org.junit.Test;

public class PrePeakYellowStampRendererTest extends BaseRendererTest {

  // From "Use of Pre-Peak Yellow Stamps in Regular Cycles (21 to 38 Days)
  // with a Mucus Cycle more thn 8 Days"

  @Test
  public void testYellowPrePeakExample() throws Exception {
    ImmutableMap.Builder<Entry, Expectations> entries = ImmutableMap.builder();
    entries.put(Entry.forText("L2ad"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("L6cx1"), Expectations.redSticker());
    entries.put(Entry.forText("L6cx2"), Expectations.redSticker());
    entries.put(Entry.forText("VL6cx2"), Expectations.redSticker());
    entries.put(Entry.forText("8cx1"), Expectations.yellowSticker());

    entries.put(Entry.forText("8cx2"), Expectations.yellowSticker());
    entries.put(Entry.forText("8cx1"), Expectations.yellowSticker());
    entries.put(Entry.forText("6cx1"), Expectations.yellowSticker());
    entries.put(Entry.forText("6cx2"), Expectations.yellowSticker());
    entries.put(Entry.forText("8kx2").pointOfChange(), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10kx3"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10klx1"), Expectations.whiteSticker().withBaby());

    entries.put(Entry.forText("10klx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10wlad"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10wlad").peakDay(), Expectations.whiteSticker().withBaby().withPeakText("P"));
    entries.put(Entry.forText("2ad"), Expectations.greenSticker().withBaby().withPeakText("1"));
    entries.put(Entry.forText("2ad"), Expectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(Entry.forText("4x1"), Expectations.greenSticker().withBaby().withPeakText("3"));
    entries.put(Entry.forText("2ad"), Expectations.greenSticker());

    entries.put(Entry.forText("2ad"), Expectations.greenSticker());
    entries.put(Entry.forText("2ad"), Expectations.greenSticker());
    entries.put(Entry.forText("2ad"), Expectations.greenSticker());

    Instructions instructions = createInstructions(
        ImmutableList.<BasicInstruction>builder().addAll(BASIC_INSTRUCTIONS.activeItems).add(BasicInstruction.K_1).build(), ImmutableList.of(), ImmutableList.of());
    runTest(entries.build(), instructions);
  }


}

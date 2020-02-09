package com.bloomcyclecare.cmcc.renderer;

import com.bloomcyclecare.cmcc.data.domain.YellowStampInstruction;
import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;

public class YellowStampLongCycleRendererTest extends BaseRendererTest {
  @Test
  public void testYellowLongCycle() throws Exception {
    ImmutableMap.Builder<BaseRendererTest.Entry, BaseRendererTest.Expectations> entries = ImmutableMap.builder();
    // 1-7
    entries.put(BaseRendererTest.Entry.forText("6GCBAD"), BaseRendererTest.Expectations.redSticker());
    entries.put(BaseRendererTest.Entry.forText("VL6BAD"), BaseRendererTest.Expectations.redSticker());
    entries.put(BaseRendererTest.Entry.forText("M"), BaseRendererTest.Expectations.redSticker());
    entries.put(BaseRendererTest.Entry.forText("M"), BaseRendererTest.Expectations.redSticker());
    entries.put(BaseRendererTest.Entry.forText("L6BGX3").intercourse(), BaseRendererTest.Expectations.redSticker());
    entries.put(BaseRendererTest.Entry.forText("L6BGX1").intercourse(), BaseRendererTest.Expectations.redSticker());
    entries.put(BaseRendererTest.Entry.forText("VL0AD").intercourse(), BaseRendererTest.Expectations.redSticker());

    // 8-14
    entries.put(BaseRendererTest.Entry.forText("6cgx1"), BaseRendererTest.Expectations.whiteSticker().withBaby());
    entries.put(BaseRendererTest.Entry.forText("6cpgx2"), BaseRendererTest.Expectations.whiteSticker().withBaby());
    entries.put(BaseRendererTest.Entry.forText("6cgx2"), BaseRendererTest.Expectations.whiteSticker().withBaby());
    entries.put(BaseRendererTest.Entry.forText("6cpx1"), BaseRendererTest.Expectations.whiteSticker().withBaby());
    entries.put(BaseRendererTest.Entry.forText("6cpx3"), BaseRendererTest.Expectations.whiteSticker().withBaby());
    entries.put(BaseRendererTest.Entry.forText("6cgpx1"), BaseRendererTest.Expectations.whiteSticker().withBaby());
    entries.put(BaseRendererTest.Entry.forText("6cgpx2"), BaseRendererTest.Expectations.whiteSticker().withBaby());

    // 15-21
    entries.put(BaseRendererTest.Entry.forText("6cgx1"), BaseRendererTest.Expectations.whiteSticker().withBaby());
    entries.put(BaseRendererTest.Entry.forText("6cx2"), BaseRendererTest.Expectations.whiteSticker().withBaby());
    entries.put(BaseRendererTest.Entry.forText("6cx1"), BaseRendererTest.Expectations.whiteSticker().withBaby());
    entries.put(BaseRendererTest.Entry.forText("6cgx2"), BaseRendererTest.Expectations.whiteSticker().withBaby());
    entries.put(BaseRendererTest.Entry.forText("10ckx1"), BaseRendererTest.Expectations.whiteSticker().withBaby());
    entries.put(BaseRendererTest.Entry.forText("8ckx1").pointOfChange(), BaseRendererTest.Expectations.whiteSticker().withBaby());
    entries.put(BaseRendererTest.Entry.forText("0ad"), BaseRendererTest.Expectations.greenSticker().withBaby().withPeakText("1"));

    // 22-28
    entries.put(BaseRendererTest.Entry.forText("0ad"), BaseRendererTest.Expectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(BaseRendererTest.Entry.forText("6cgx1"), BaseRendererTest.Expectations.yellowSticker().withBaby().withPeakText("3"));
    entries.put(BaseRendererTest.Entry.forText("6cx1"), BaseRendererTest.Expectations.yellowSticker());
    entries.put(BaseRendererTest.Entry.forText("6cgx1"), BaseRendererTest.Expectations.yellowSticker());
    entries.put(BaseRendererTest.Entry.forText("8ckx1").pointOfChange(), BaseRendererTest.Expectations.whiteSticker().withBaby());
    // TODO: check with becky that this should be yellow baby
    // TODO: is POC always a N for ESQ?
    // TODO:
    entries.put(BaseRendererTest.Entry.forText("6cgx1"), BaseRendererTest.Expectations.yellowSticker().withBaby().withPeakText("1"));
    entries.put(BaseRendererTest.Entry.forText("10kx1").pointOfChange(), BaseRendererTest.Expectations.whiteSticker().withBaby());

    // 29-35
    entries.put(BaseRendererTest.Entry.forText("8ckx1"), BaseRendererTest.Expectations.yellowSticker().withBaby().withPeakText("1"));
    entries.put(BaseRendererTest.Entry.forText("0ad").pointOfChange(), BaseRendererTest.Expectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(BaseRendererTest.Entry.forText("6cad"), BaseRendererTest.Expectations.yellowSticker().withBaby().withPeakText("3"));
    Instructions longCycleInstructions =
        createInstructions(ImmutableList.of(), ImmutableList.of(), ImmutableList.of(
            YellowStampInstruction.YS_1_A, YellowStampInstruction.YS_1_B, YellowStampInstruction.YS_1_C, YellowStampInstruction.YS_1_D,
            YellowStampInstruction.YS_2_A, YellowStampInstruction.YS_2_B, YellowStampInstruction.YS_3_B));

    //runTest(entries.build(), BASIC_INSTRUCTIONS, longCycleInstructions);
  }
}

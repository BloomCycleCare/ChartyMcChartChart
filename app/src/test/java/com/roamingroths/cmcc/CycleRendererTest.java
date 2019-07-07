package com.roamingroths.cmcc;

import android.content.Context;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.StandardSubjectBuilder;
import com.roamingroths.cmcc.data.domain.BasicInstruction;
import com.roamingroths.cmcc.data.domain.Observation;
import com.roamingroths.cmcc.data.domain.YellowStampInstruction;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.data.entities.Instructions;
import com.roamingroths.cmcc.data.entities.ObservationEntry;
import com.roamingroths.cmcc.data.models.ChartEntry;
import com.roamingroths.cmcc.logic.chart.CycleRenderer;
import com.roamingroths.cmcc.logic.chart.StickerColor;

import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.reactivex.functions.Predicate;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Created by parkeroth on 7/1/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class CycleRendererTest {

  private static final LocalDate CYCLE_START_DATE = LocalDate.parse("2017-01-01");
  private static final String CYCLE_ID = "fake-cycleToShow-id";
  private static final boolean PREPEAK_YELLOW_ENABLED = true;
  private static final boolean PREPEAK_YELLOW_DISABLED = false;
  private static final boolean POSTPEAK_YELLOW_ENABLED = true;
  private static final boolean POSTPEAK_YELLOW_DISABLED = false;
  private static final Instructions BASIC_INSTRUCTIONS = new Instructions(
      CYCLE_START_DATE,
      ImmutableList.of(BasicInstruction.D_2, BasicInstruction.D_4, BasicInstruction.D_5, BasicInstruction.D_6),
      ImmutableList.of(), ImmutableList.of());

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
  // TODO: B1D
  // TODO: B1E

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
    Instructions instructions =
        new Instructions(CYCLE_START_DATE, ImmutableList.of(BasicInstruction.K_1), ImmutableList.of(), ImmutableList.of());
    runTest(entries.build(), instructions);
  }

  @Test
  public void testYellowPostPeakExample() throws Exception {
    ImmutableMap.Builder<Entry, Expectations> entries = ImmutableMap.builder();
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("H"), Expectations.redSticker());
    entries.put(Entry.forText("L2ad"), Expectations.redSticker());
    entries.put(Entry.forText("VL0ad"), Expectations.redSticker());
    entries.put(Entry.forText("0ad").intercourse(), Expectations.greenSticker().withIntercourse());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("2ad").intercourse(), Expectations.greenSticker().withIntercourse());

    entries.put(Entry.forText("6cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("8cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10cgx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10cklx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10klx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10cklad"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("4x2"), Expectations.greenSticker().withBaby().withPeakText("1"));

    entries.put(Entry.forText("10cx1").peakDay(), Expectations.whiteSticker().withBaby().withPeakText("P"));
    entries.put(Entry.forText("8cx2"), Expectations.yellowSticker().withBaby().withPeakText("1"));
    entries.put(Entry.forText("8cgx2"), Expectations.yellowSticker().withBaby().withPeakText("2"));
    entries.put(Entry.forText("8gyx1"), Expectations.yellowSticker().withBaby().withPeakText("3"));
    entries.put(Entry.forText("8cx1"), Expectations.yellowSticker());
    entries.put(Entry.forText("6cx1"), Expectations.yellowSticker());
    entries.put(Entry.forText("6cx1"), Expectations.yellowSticker());

    entries.put(Entry.forText("8cx2"), Expectations.yellowSticker());
    entries.put(Entry.forText("8cx2"), Expectations.yellowSticker());
    entries.put(Entry.forText("4ad"), Expectations.greenSticker());
    entries.put(Entry.forText("8cx1"), Expectations.yellowSticker());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker());
    entries.put(Entry.forText("6cx1"), Expectations.yellowSticker());
    entries.put(Entry.forText("8cx1"), Expectations.yellowSticker());
    Instructions instructions =
        new Instructions(CYCLE_START_DATE, ImmutableList.<BasicInstruction>builder().addAll(BASIC_INSTRUCTIONS.activeItems).add(BasicInstruction.K_2).build(), ImmutableList.of(), ImmutableList.of());
    runTest(entries.build(), instructions);
  }

  @Test
  public void testYellowLongCycle() throws Exception {
    ImmutableMap.Builder<Entry, Expectations> entries = ImmutableMap.builder();
    // 1-7
    entries.put(Entry.forText("6GCBAD"), Expectations.redSticker());
    entries.put(Entry.forText("VL6BAD"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("M"), Expectations.redSticker());
    entries.put(Entry.forText("L6BGX3").intercourse(), Expectations.redSticker());
    entries.put(Entry.forText("L6BGX1").intercourse(), Expectations.redSticker());
    entries.put(Entry.forText("VL0AD").intercourse(), Expectations.redSticker());

    // 8-14
    entries.put(Entry.forText("6cgx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("6cpgx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("6cgx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("6cpx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("6cpx3"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("6cgpx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("6cgpx2"), Expectations.whiteSticker().withBaby());

    // 15-21
    entries.put(Entry.forText("6cgx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("6cx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("6cx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("6cgx2"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("10ckx1"), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("8ckx1").pointOfChange(), Expectations.whiteSticker().withBaby());
    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("1"));

    // 22-28
    entries.put(Entry.forText("0ad"), Expectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(Entry.forText("6cgx1"), Expectations.yellowSticker().withBaby().withPeakText("3"));
    entries.put(Entry.forText("6cx1"), Expectations.yellowSticker());
    entries.put(Entry.forText("6cgx1"), Expectations.yellowSticker());
    entries.put(Entry.forText("8ckx1").pointOfChange(), Expectations.whiteSticker().withBaby());
    // TODO: check with becky that this should be yellow baby
    // TODO: is POC always a N for ESQ?
    // TODO:
    entries.put(Entry.forText("6cgx1"), Expectations.yellowSticker().withBaby().withPeakText("1"));
    entries.put(Entry.forText("10kx1").pointOfChange(), Expectations.whiteSticker().withBaby());

    // 29-35
    entries.put(Entry.forText("8ckx1"), Expectations.yellowSticker().withBaby().withPeakText("1"));
    entries.put(Entry.forText("0ad").pointOfChange(), Expectations.greenSticker().withBaby().withPeakText("2"));
    entries.put(Entry.forText("6cad"), Expectations.yellowSticker().withBaby().withPeakText("3"));
    Instructions longCycleInstructions =
        new Instructions(CYCLE_START_DATE.plusDays(20), ImmutableList.of(), ImmutableList.of(), ImmutableList.of(
            YellowStampInstruction.YS_1_A, YellowStampInstruction.YS_1_B, YellowStampInstruction.YS_1_C, YellowStampInstruction.YS_1_D,
            YellowStampInstruction.YS_2_A, YellowStampInstruction.YS_2_B, YellowStampInstruction.YS_3_B));

    runTest(entries.build(), BASIC_INSTRUCTIONS, longCycleInstructions);
  }

  private void runTest(
      ImmutableMap<Entry, Expectations> entries, Instructions... instructions) throws Exception {
    int numEntries = entries.size();
    List<ChartEntry> chartEntries = new ArrayList<>(numEntries);
    List<Predicate<CycleRenderer.RenderableEntry>> tests = new ArrayList<>(numEntries);
    for (Map.Entry<Entry, Expectations> anEntry : entries.entrySet()) {
      LocalDate entryDate = CYCLE_START_DATE.plusDays(chartEntries.size());
      chartEntries.add(new ChartEntry(entryDate, anEntry.getKey().asChartEntry(entryDate), null, null));
      Expectations expectations = anEntry.getValue();
      tests.add(renderableEntry -> {
        StandardSubjectBuilder baseAssert = assertWithMessage("Issue on " + entryDate);
        baseAssert
            .withMessage("showBaby")
            .that(renderableEntry.showBaby)
            .isEqualTo(expectations.shouldHaveBaby);
        baseAssert
            .withMessage("backgroundColor")
            .that(renderableEntry.backgroundColor)
            .isEqualTo(expectations.backgroundColor);
        baseAssert
            .withMessage("peakDayText")
            .that(renderableEntry.peakDayText)
            .isEqualTo(expectations.peakText);
        if (expectations.shouldHaveIntercourse) {
          baseAssert
              .withMessage("intercourse")
              .that(renderableEntry.entrySummary).endsWith("I");
        }
        return true;
      });
    }
    Cycle cycle = new Cycle("", CYCLE_START_DATE, null);

    List<CycleRenderer.RenderableEntry> renderableEntries =
        new CycleRenderer(cycle, chartEntries, Arrays.asList(instructions)).render();

    Preconditions.checkState(renderableEntries.size() == numEntries);
    for (int i=0; i<numEntries; i++) {
      tests.get(i).test(renderableEntries.get(i));
    }
  }

  private static class Entry {

    private final String observationText;
    private boolean peakDay = false;
    private boolean intercourse = false;
    private boolean pointOfChange = false;
    private boolean unusualBleeding = false;

    private Entry(String observationText) {
      this.observationText = observationText;
    }

    public static Entry forText(String observationText) {
      return new Entry(observationText);
    }

    public Entry peakDay() {
      peakDay = true;
      return this;
    }

    public Entry intercourse() {
      intercourse = true;
      return this;
    }

    public Entry pointOfChange() {
      pointOfChange = true;
      return this;
    }

    public Entry unusualBleeding() {
      unusualBleeding = true;
      return this;
    }

    public ObservationEntry asChartEntry(LocalDate date) throws Observation.InvalidObservationException {
      Observation observation = Observation.fromString(observationText);
      return new ObservationEntry(date, observation, peakDay, intercourse, false, pointOfChange, unusualBleeding, null, false);
    }
  }

  private static class Expectations {

    public final StickerColor backgroundColor;
    public boolean shouldHaveBaby = false;
    public boolean shouldHaveIntercourse = false;
    public String peakText = "";

    private Expectations(StickerColor backgroundColor) {
      this.backgroundColor = backgroundColor;
    }

    public static Expectations redSticker() {
      return new Expectations(StickerColor.RED);
    }

    public static Expectations greenSticker() {
      return new Expectations(StickerColor.GREEN);
    }

    public static Expectations yellowSticker() {
      return new Expectations(StickerColor.YELLOW);
    }

    public static Expectations whiteSticker() {
      return new Expectations(StickerColor.WHITE);
    }

    public Expectations withPeakText(String text) {
      peakText = text;
      return this;
    }

    public Expectations withBaby() {
      shouldHaveBaby = true;
      return this;
    }

    public Expectations withIntercourse() {
      shouldHaveIntercourse = true;
      return this;
    }
  }
}

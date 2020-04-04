package com.bloomcyclecare.cmcc.renderer;

import com.bloomcyclecare.cmcc.data.domain.BasicInstruction;
import com.bloomcyclecare.cmcc.data.domain.Observation;
import com.bloomcyclecare.cmcc.data.domain.SpecialInstruction;
import com.bloomcyclecare.cmcc.data.domain.YellowStampInstruction;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.bloomcyclecare.cmcc.data.entities.ObservationEntry;
import com.bloomcyclecare.cmcc.data.models.ChartEntry;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.logic.chart.ObservationParser;
import com.bloomcyclecare.cmcc.logic.chart.StickerColor;
import com.bloomcyclecare.cmcc.utils.GsonUtil;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.StandardSubjectBuilder;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.reactivex.functions.Predicate;

import static com.google.common.truth.Truth.assertWithMessage;

public abstract class BaseRendererTest {

  static final LocalDate CYCLE_START_DATE = LocalDate.parse("2017-01-01");

  static final Instructions BASIC_INSTRUCTIONS = createInstructions(
      ImmutableList.of(
          BasicInstruction.D_1, BasicInstruction.D_2, BasicInstruction.D_3, BasicInstruction.D_4, BasicInstruction.D_5, BasicInstruction.D_6,
          BasicInstruction.E_1, /*BasicInstruction.E_2,*/ BasicInstruction.E_3, BasicInstruction.E_4, /*BasicInstruction.E_5, BasicInstruction.E_6,*/ BasicInstruction.E_7),
      ImmutableList.of(), ImmutableList.of());

  static class Entry {

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

    public ObservationEntry asChartEntry(LocalDate date) throws ObservationParser.InvalidObservationException {
      Observation observation = ObservationParser.parse(observationText).orNull();
      return new ObservationEntry(date, observation, peakDay, intercourse, false, false, pointOfChange, unusualBleeding, null, false, "");
    }
  }

  static class Expectations {

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

    public static Expectations greySticker() {
      return new Expectations(StickerColor.GREY);
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

  void runTest(
      ImmutableMap<Entry, Expectations> entries, Instructions... instructions) throws Exception {
    int numEntries = entries.size();
    List<ChartEntry> chartEntries = new ArrayList<>(numEntries);
    List<Predicate<CycleRenderer.RenderableEntry>> tests = new ArrayList<>(numEntries);
    for (Map.Entry<Entry, Expectations> anEntry : entries.entrySet()) {
      LocalDate entryDate = CYCLE_START_DATE.plusDays(chartEntries.size());
      chartEntries.add(new ChartEntry(entryDate, anEntry.getKey().asChartEntry(entryDate), null, null));
      Expectations expectations = anEntry.getValue();
      tests.add(renderableEntry -> {
        StandardSubjectBuilder baseAssert = assertWithMessage(String.format("Issue on %s %s", entryDate, GsonUtil.getGsonInstance().toJson(renderableEntry)));
        baseAssert
            .withMessage("backgroundColor")
            .that(renderableEntry.backgroundColor)
            .isEqualTo(expectations.backgroundColor);
        baseAssert
            .withMessage("showBaby")
            .that(renderableEntry.showBaby)
            .isEqualTo(expectations.shouldHaveBaby);
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
    Cycle cycle = new Cycle("", CYCLE_START_DATE, null, null);

    Optional<Cycle> previousCycle = Optional.absent();
    List<CycleRenderer.RenderableEntry> renderableEntries =
        new CycleRenderer(cycle, previousCycle, chartEntries, Arrays.asList(instructions)).render().entries;

    Preconditions.checkState(renderableEntries.size() == numEntries);
    for (int i=0; i<numEntries; i++) {
      tests.get(i).test(renderableEntries.get(i));
    }
  }

  static Instructions createInstructions(List<BasicInstruction> basicInstructions,
                                  List<SpecialInstruction> specialInstructions,
                                  List<YellowStampInstruction> yellowStampInstructions) {
    return new Instructions(CYCLE_START_DATE, basicInstructions, specialInstructions, yellowStampInstructions);
  }
}

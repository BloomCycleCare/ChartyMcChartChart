package com.bloomcyclecare.cmcc.renderer;

import com.bloomcyclecare.cmcc.OptionalStringSubject;
import com.bloomcyclecare.cmcc.data.models.charting.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.charting.Cycle;
import com.bloomcyclecare.cmcc.data.models.instructions.BasicInstruction;
import com.bloomcyclecare.cmcc.data.models.instructions.Instructions;
import com.bloomcyclecare.cmcc.data.models.instructions.SpecialInstruction;
import com.bloomcyclecare.cmcc.data.models.instructions.YellowStampInstruction;
import com.bloomcyclecare.cmcc.data.models.training.StickerExpectations;
import com.bloomcyclecare.cmcc.data.models.training.TrainingCycle;
import com.bloomcyclecare.cmcc.data.models.training.TrainingEntry;
import com.bloomcyclecare.cmcc.data.utils.GsonUtil;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.logic.chart.ObservationParser;
import com.bloomcyclecare.cmcc.logic.chart.StickerSelector;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.truth.StandardSubjectBuilder;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.reactivex.functions.Predicate;

import static com.google.common.truth.Truth.assertWithMessage;

public abstract class BaseRendererTest {

  static final LocalDate CYCLE_START_DATE = LocalDate.parse("2017-01-01");
  static final Instructions BASIC_INSTRUCTIONS = Instructions.createBasicInstructions(CYCLE_START_DATE);

  void runTest(TrainingCycle trainingCycle) throws Exception {
    runTest(trainingCycle.entries(), trainingCycle.instructions);
  }

  @Deprecated
  void runTest(
      ImmutableMap<TrainingEntry, Optional<StickerExpectations>> entries, Instructions instructions) throws Exception {
    int numEntries = entries.size();
    List<ChartEntry> chartEntries = new ArrayList<>(numEntries);
    List<Predicate<CycleRenderer.RenderableEntry>> tests = new ArrayList<>(numEntries);
    for (Map.Entry<TrainingEntry, Optional<StickerExpectations>> anEntry : entries.entrySet()) {
      LocalDate entryDate = CYCLE_START_DATE.plusDays(chartEntries.size());
      chartEntries.add(new ChartEntry(entryDate, anEntry.getKey().asChartEntry(entryDate, observation -> {
        try {
          return ObservationParser.parse(observation);
        } catch (Exception e) {
          throw new IllegalStateException(e);
        }
      }), null, null, null, null));
      Optional<StickerExpectations> stickerExpectations = anEntry.getValue();
      stickerExpectations.ifPresent(expectations -> tests.add(renderableEntry -> {
        StandardSubjectBuilder baseAssert = assertWithMessage(
            String.format("Issue on %s %s", entryDate, GsonUtil.getGsonInstance().toJson(renderableEntry)));
        baseAssert
            .withMessage("stickerSelection")
            .withMessage("Matched Criteria: " + StickerSelector.select(renderableEntry.stickerSelectionContext()).matchedCriteria)
            .that(renderableEntry.expectedStickerSelection())
            .isEqualTo(expectations.stickerSelection);
        if (expectations.shouldHaveIntercourse) {
          baseAssert
              .withMessage("intercourse")
              .about(OptionalStringSubject.optionals())
              .that(renderableEntry.entrySummary()).value().endsWith("I");
        }
        return true;
      }));
    }
    Cycle cycle = new Cycle("", CYCLE_START_DATE, null, null);

    Optional<Cycle> previousCycle = Optional.empty();
    Instructions copyOfInstructions = new Instructions(instructions);
    copyOfInstructions.startDate = CYCLE_START_DATE;
    List<CycleRenderer.RenderableEntry> renderableEntries = new CycleRenderer(cycle, previousCycle, chartEntries, ImmutableSet.of(copyOfInstructions))
        .render().entries();

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

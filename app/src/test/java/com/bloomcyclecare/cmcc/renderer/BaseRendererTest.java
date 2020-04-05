package com.bloomcyclecare.cmcc.renderer;

import com.bloomcyclecare.cmcc.data.domain.BasicInstruction;
import com.bloomcyclecare.cmcc.data.domain.SpecialInstruction;
import com.bloomcyclecare.cmcc.data.domain.YellowStampInstruction;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.bloomcyclecare.cmcc.data.models.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.TrainingCycle;
import com.bloomcyclecare.cmcc.data.models.TrainingEntry;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.utils.GsonUtil;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.truth.StandardSubjectBuilder;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
      ImmutableMap<TrainingEntry, TrainingCycle.StickerExpectations> entries, Instructions instructions) throws Exception {
    int numEntries = entries.size();
    List<ChartEntry> chartEntries = new ArrayList<>(numEntries);
    List<Predicate<CycleRenderer.RenderableEntry>> tests = new ArrayList<>(numEntries);
    for (Map.Entry<TrainingEntry, TrainingCycle.StickerExpectations> anEntry : entries.entrySet()) {
      LocalDate entryDate = CYCLE_START_DATE.plusDays(chartEntries.size());
      chartEntries.add(new ChartEntry(entryDate, anEntry.getKey().asChartEntry(entryDate), null, null));
      TrainingCycle.StickerExpectations stickerExpectations = anEntry.getValue();
      tests.add(renderableEntry -> {
        StandardSubjectBuilder baseAssert = assertWithMessage(String.format("Issue on %s %s", entryDate, GsonUtil.getGsonInstance().toJson(renderableEntry)));
        baseAssert
            .withMessage("backgroundColor")
            .that(renderableEntry.backgroundColor)
            .isEqualTo(stickerExpectations.backgroundColor);
        baseAssert
            .withMessage("showBaby")
            .that(renderableEntry.showBaby)
            .isEqualTo(stickerExpectations.shouldHaveBaby);
        baseAssert
            .withMessage("peakDayText")
            .that(renderableEntry.peakDayText)
            .isEqualTo(stickerExpectations.peakText);
        if (stickerExpectations.shouldHaveIntercourse) {
          baseAssert
              .withMessage("intercourse")
              .that(renderableEntry.entrySummary).endsWith("I");
        }
        return true;
      });
    }
    Cycle cycle = new Cycle("", CYCLE_START_DATE, null, null);

    Optional<Cycle> previousCycle = Optional.absent();
    Instructions copyOfInstructions = new Instructions(instructions);
    copyOfInstructions.startDate = CYCLE_START_DATE;
    List<CycleRenderer.RenderableEntry> renderableEntries = new CycleRenderer(cycle, previousCycle, chartEntries, ImmutableSet.of(copyOfInstructions))
        .render().entries;

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

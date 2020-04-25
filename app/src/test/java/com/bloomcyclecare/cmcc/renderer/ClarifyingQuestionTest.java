package com.bloomcyclecare.cmcc.renderer;

import com.bloomcyclecare.cmcc.data.domain.BasicInstruction;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.bloomcyclecare.cmcc.data.models.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.TrainingEntry;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.utils.GsonUtil;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.StandardSubjectBuilder;

import org.joda.time.LocalDate;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

import static com.google.common.truth.Truth.assertWithMessage;

public class ClarifyingQuestionTest extends BaseRendererTest {

  @Test
  public void essentialSamenessQuestion() throws Exception {
    ImmutableMap.Builder<TrainingEntry, Boolean> entries = ImmutableMap.builder();
    entries.put(TrainingEntry.forText("8BCX1"), false);
    entries.put(TrainingEntry.forText("H"), true);
    entries.put(TrainingEntry.forText("M"), true);
    entries.put(TrainingEntry.forText("L6x1"), true);
    entries.put(TrainingEntry.forText("2x1"), true);
    entries.put(TrainingEntry.forText("0AD"), true);
    entries.put(TrainingEntry.forText("0AD"), true);

    entries.put(TrainingEntry.forText("0AD"), true);
    entries.put(TrainingEntry.forText("6cx2"), true);
    entries.put(TrainingEntry.forText("8cx2"), true);
    entries.put(TrainingEntry.forText("8kx2"), true);
    entries.put(TrainingEntry.forText("10klAD"), true);
    entries.put(TrainingEntry.forText("10klAD").peakDay(), false);
    entries.put(TrainingEntry.forText("6cx1"), false);

    entries.put(TrainingEntry.forText("0AD"), false);
    entries.put(TrainingEntry.forText("4x1"), false);
    entries.put(TrainingEntry.forText("0AD"), false);
    entries.put(TrainingEntry.forText("0AD"), false);
    entries.put(TrainingEntry.forText("2x1"), false);

    Instructions instructions =
        createInstructions(ImmutableList.<BasicInstruction>builder()
            .addAll(BASIC_INSTRUCTIONS.activeItems)
            .add(BasicInstruction.K_1)
            .build(), ImmutableList.of(), ImmutableList.of());
    run(entries.build(), (re) -> re.modificationContext().shouldAskEssentialSamenessIfMucus, instructions);
  }

  @Test
  public void doublePeakQuestionDisabled() throws Exception {
    ImmutableMap.Builder<TrainingEntry, Boolean> entries = ImmutableMap.builder();
    entries.put(TrainingEntry.forText("8BCX1"), false);
    entries.put(TrainingEntry.forText("H"), false);
    entries.put(TrainingEntry.forText("M"), false);
    entries.put(TrainingEntry.forText("L6x1"), false);
    entries.put(TrainingEntry.forText("2x1"), false);
    entries.put(TrainingEntry.forText("0AD"), false);
    entries.put(TrainingEntry.forText("0AD"), false);

    entries.put(TrainingEntry.forText("0AD"), false);
    entries.put(TrainingEntry.forText("6cx2"), false);
    entries.put(TrainingEntry.forText("8cx2"), false);
    entries.put(TrainingEntry.forText("8kx2"), false);
    entries.put(TrainingEntry.forText("10klAD"), false);
    entries.put(TrainingEntry.forText("10klAD").peakDay(), false);
    entries.put(TrainingEntry.forText("6cx1"), false);

    entries.put(TrainingEntry.forText("0AD"), false);
    entries.put(TrainingEntry.forText("4x1"), false);
    entries.put(TrainingEntry.forText("0AD"), false);
    entries.put(TrainingEntry.forText("0AD"), false);
    entries.put(TrainingEntry.forText("2x1"), false);

    run(entries.build(), (re) -> re.modificationContext().shouldAskDoublePeakQuestions, BASIC_INSTRUCTIONS);
  }

  @Test
  public void doublePeakQuestionEnabled() throws Exception {
    ImmutableMap.Builder<TrainingEntry, Boolean> entries = ImmutableMap.builder();
    entries.put(TrainingEntry.forText("8BCX1"), false);
    entries.put(TrainingEntry.forText("H"), false);
    entries.put(TrainingEntry.forText("M"), false);
    entries.put(TrainingEntry.forText("L6x1"), false);
    entries.put(TrainingEntry.forText("2x1"), false);
    entries.put(TrainingEntry.forText("0AD"), false);
    entries.put(TrainingEntry.forText("0AD"), false);

    entries.put(TrainingEntry.forText("0AD"), false);
    entries.put(TrainingEntry.forText("6cx2"), false);
    entries.put(TrainingEntry.forText("8cx2"), false);
    entries.put(TrainingEntry.forText("8kx2"), false);
    entries.put(TrainingEntry.forText("10klAD"), false);
    entries.put(TrainingEntry.forText("10klAD").peakDay(), false);
    entries.put(TrainingEntry.forText("6cx1"), false);

    entries.put(TrainingEntry.forText("0AD"), false);
    entries.put(TrainingEntry.forText("4x1"), true);
    entries.put(TrainingEntry.forText("0AD"), false);
    entries.put(TrainingEntry.forText("0AD"), false);
    entries.put(TrainingEntry.forText("2x1"), false);

    Instructions instructions =
        createInstructions(ImmutableList.<BasicInstruction>builder()
            .addAll(BASIC_INSTRUCTIONS.activeItems)
            .add(BasicInstruction.G_1)
            .build(), ImmutableList.of(), ImmutableList.of());
    run(entries.build(), (re) -> re.modificationContext().shouldAskDoublePeakQuestions, instructions);
  }

  private void run(
      ImmutableMap<TrainingEntry, Boolean> entries,
      Function<CycleRenderer.RenderableEntry, Boolean> valueProvider,
      Instructions... instructions) throws Exception {
    int numEntries = entries.size();
    List<ChartEntry> chartEntries = new ArrayList<>(numEntries);
    List<Predicate<CycleRenderer.RenderableEntry>> tests = new ArrayList<>(numEntries);
    for (Map.Entry<TrainingEntry, Boolean> anEntry : entries.entrySet()) {
      LocalDate entryDate = CYCLE_START_DATE.plusDays(chartEntries.size());
      chartEntries.add(new ChartEntry(entryDate, anEntry.getKey().asChartEntry(entryDate), null, null, null));
      tests.add(renderableEntry -> {
        StandardSubjectBuilder baseAssert = assertWithMessage(String.format("Issue on %s %s", entryDate, GsonUtil.getGsonInstance().toJson(renderableEntry)));
        baseAssert
            .that(valueProvider.apply(renderableEntry))
            .isEqualTo(anEntry.getValue());
        return true;
      });
    }
    Cycle cycle = new Cycle("", CYCLE_START_DATE, null, null);

    List<CycleRenderer.RenderableEntry> renderableEntries =
        new CycleRenderer(cycle, Optional.empty(), chartEntries, Arrays.asList(instructions)).render().entries();

    Preconditions.checkState(renderableEntries.size() == numEntries);
    for (int i = 0; i < numEntries; i++) {
      tests.get(i).test(renderableEntries.get(i));
    }
  }
}

package com.bloomcyclecare.cmcc.data.repos.entry;

import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.bloomcyclecare.cmcc.data.entities.SymptomEntry;
import com.bloomcyclecare.cmcc.data.entities.WellnessEntry;
import com.bloomcyclecare.cmcc.data.models.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.StickerSelection;
import com.bloomcyclecare.cmcc.data.models.TrainingCycle;
import com.bloomcyclecare.cmcc.data.models.TrainingEntry;
import com.bloomcyclecare.cmcc.logic.chart.ObservationParser;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;

import static com.google.common.truth.Truth.assertThat;

public class TrainingChartEntryRepoTest {

  /*
  Flowable<List<ChartEntry>> getStreamForCycle(Flowable<Cycle> cycleStream);
  */

  @Test
  public void testGetAllEntries() throws Exception {
    TrainingChartEntryRepo repo = new TrainingChartEntryRepo(trainingCycles(), true);

    assertThat(repo.getAllEntries().blockingGet()).containsExactlyElementsIn(expectedEntries()).inOrder();
  }

  @Test
  public void testGetStreamForCycle() throws Exception {
    TrainingChartEntryRepo repo = new TrainingChartEntryRepo(trainingCycles(), true);

    List<ChartEntry> expectedEntries = expectedEntries();

    Cycle cycle = new Cycle("testing", LocalDate.now().minusDays(2), null, null);
    Cycle updatedCycle = new Cycle(cycle);
    updatedCycle.endDate = LocalDate.now().minusDays(1);

    assertThat(repo.getStreamForCycle(Flowable.fromArray(cycle, updatedCycle)).blockingIterable()).containsExactly(
        ImmutableList.of(expectedEntries.get(1), expectedEntries.get(2), expectedEntries.get(3)),
        ImmutableList.of(expectedEntries.get(1), expectedEntries.get(2))).inOrder();
  }

  @Test
  public void testLatestN() throws Exception {
    TrainingChartEntryRepo repo = new TrainingChartEntryRepo(trainingCycles(), true);

    List<ChartEntry> expectedEntries = expectedEntries();

    assertThat(repo.getLatestN(0).blockingFirst()).isEmpty();
    assertThat(repo.getLatestN(1).blockingFirst()).containsExactly(expectedEntries.get(3));
    assertThat(repo.getLatestN(2).blockingFirst()).containsExactlyElementsIn(expectedEntries.subList(2, 4)).inOrder();
    assertThat(repo.getLatestN(3).blockingFirst()).containsExactlyElementsIn(expectedEntries.subList(1, 4)).inOrder();
    assertThat(repo.getLatestN(4).blockingFirst()).containsExactlyElementsIn(expectedEntries.subList(0, 4)).inOrder();
    assertThat(repo.getLatestN(5).blockingFirst()).containsExactlyElementsIn(expectedEntries.subList(0, 4)).inOrder();
  }

  @Test
  public void testUpdateStickerSelection() throws Exception {
    TrainingChartEntryRepo repo = new TrainingChartEntryRepo(trainingCycles(), true);

    ChartEntry firstEntry = repo.getLatestN(1).blockingFirst().get(0);
    assertThat(firstEntry.stickerSelection).isEqualTo(StickerSelection.create(StickerSelection.Sticker.RED, null));

    StickerSelection expected = StickerSelection.create(StickerSelection.Sticker.GREEN_BABY, StickerSelection.Text.P);
    repo.updateStickerSelection(firstEntry.entryDate, expected).test().assertComplete();

    ChartEntry updatedEntry = repo.getLatestN(1).blockingFirst().get(0);
    assertThat(updatedEntry.stickerSelection).isEqualTo(expected);
  }

  private List<ChartEntry> expectedEntries() {
    List<ChartEntry> entries = new ArrayList<>();

    entries.add(createEntry("H", LocalDate.now().minusDays(3), StickerSelection.create(StickerSelection.Sticker.RED, null)));
    entries.add(createEntry("M", LocalDate.now().minusDays(2), StickerSelection.create(StickerSelection.Sticker.RED, null)));
    entries.add(createEntry("L0AD", LocalDate.now().minusDays(1), StickerSelection.create(StickerSelection.Sticker.RED, null)));
    entries.add(createEntry("VL0AD", LocalDate.now(), StickerSelection.create(StickerSelection.Sticker.RED, null)));

    return entries;
  }

  private ChartEntry createEntry(String entryText, LocalDate entryDate, StickerSelection stickerSelection) {
    try {
      return new ChartEntry(entryDate, TrainingEntry.forText(entryText).asChartEntry(entryDate), WellnessEntry.emptyEntry(entryDate), SymptomEntry.emptyEntry(entryDate), stickerSelection);
    } catch (ObservationParser.InvalidObservationException ioe) {
      throw new IllegalStateException(ioe);
    }
  }


  private List<TrainingCycle> trainingCycles() {
    TrainingCycle cycle1 = TrainingCycle.withInstructions(Instructions.createBasicInstructions(LocalDate.now()));
    cycle1.addEntry(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker());
    cycle1.addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker());

    TrainingCycle cycle2 = TrainingCycle.withInstructions(Instructions.createBasicInstructions(LocalDate.now()));
    cycle2.addEntry(TrainingEntry.forText("L0AD"), TrainingCycle.StickerExpectations.redSticker());
    cycle2.addEntry(TrainingEntry.forText("VL0AD"), TrainingCycle.StickerExpectations.redSticker());

    return ImmutableList.of(cycle1, cycle2);
  }
}

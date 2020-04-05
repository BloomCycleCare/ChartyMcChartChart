package com.bloomcyclecare.cmcc.data.repos.cycle;

import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.bloomcyclecare.cmcc.data.models.TrainingCycle;
import com.bloomcyclecare.cmcc.data.models.TrainingEntry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.joda.time.LocalDate;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class TrainingCycleRepoTest {

  @Test
  public void testGetStream() {
    TrainingCycleRepo repo = new TrainingCycleRepo(trainingCycles(), LocalDate::now);
    assertThat(repo.getStream().blockingFirst()).containsExactlyElementsIn(expectedCycles()).inOrder();
  }

  @Test
  public void testGetCurrentCycle() {
    TrainingCycleRepo repo = new TrainingCycleRepo(trainingCycles(), LocalDate::now);
    assertThat(repo.getLatestCycle().blockingGet()).isEqualTo(Iterables.getLast(expectedCycles()));
  }

  @Test
  public void testGetLatestCycle() {
    TrainingCycleRepo repo = new TrainingCycleRepo(trainingCycles(), LocalDate::now);
    assertThat(repo.getLatestCycle().blockingGet()).isEqualTo(Iterables.getLast(expectedCycles()));
  }

  @Test
  public void testGetPrevious() {
    TrainingCycleRepo repo = new TrainingCycleRepo(trainingCycles(), LocalDate::now);
    List<Cycle> cycles = expectedCycles();

    assertThat(repo.getPreviousCycle(cycles.get(2)).blockingGet()).isEqualTo(cycles.get(1));
    assertThat(repo.getPreviousCycle(cycles.get(1)).blockingGet()).isEqualTo(cycles.get(0));
    repo.getPreviousCycle(cycles.get(0)).test().assertResult();
  }

  @Test
  public void testGetNext() {
    TrainingCycleRepo repo = new TrainingCycleRepo(trainingCycles(), LocalDate::now);
    List<Cycle> cycles = expectedCycles();

    assertThat(repo.getNextCycle(cycles.get(0)).blockingGet()).isEqualTo(cycles.get(1));
    assertThat(repo.getNextCycle(cycles.get(1)).blockingGet()).isEqualTo(cycles.get(2));
    repo.getNextCycle(cycles.get(2)).test().assertResult();
  }

  @Test
  public void testGetCycleForDate() {
    TrainingCycleRepo repo = new TrainingCycleRepo(trainingCycles(), LocalDate::now);
    List<Cycle> cycles = expectedCycles();

    LocalDate date = cycles.get(0).startDate.minusDays(1);
    repo.getCycleForDate(date).test().assertResult();

    date = date.plusDays(1);
    assertThat(repo.getCycleForDate(date).blockingGet()).isEqualTo(cycles.get(0));
    date = date.plusDays(1);
    assertThat(repo.getCycleForDate(date).blockingGet()).isEqualTo(cycles.get(0));
    date = date.plusDays(1);
    assertThat(repo.getCycleForDate(date).blockingGet()).isEqualTo(cycles.get(0));

    date = date.plusDays(1);
    assertThat(repo.getCycleForDate(date).blockingGet()).isEqualTo(cycles.get(1));
    date = date.plusDays(1);
    assertThat(repo.getCycleForDate(date).blockingGet()).isEqualTo(cycles.get(1));

    date = date.plusDays(1);
    assertThat(repo.getCycleForDate(date).blockingGet()).isEqualTo(cycles.get(2));

    date = date.plusDays(1);
    assertThat(repo.getCycleForDate(date).blockingGet()).isEqualTo(cycles.get(2));
  }

  @Test
  public void testUpdateEvents() {
    TrainingCycleRepo repo = new TrainingCycleRepo(trainingCycles(), LocalDate::now);

    repo.updateEvents().test().assertResult().dispose();
  }

  private static List<Cycle> expectedCycles() {
    List<Cycle> cycles = new ArrayList<>();
    cycles.add(new Cycle("training", LocalDate.now().minusDays(5), LocalDate.now().minusDays(3), null));
    cycles.add(new Cycle("training", LocalDate.now().minusDays(2), LocalDate.now().minusDays(1), null));
    cycles.add(new Cycle("training", LocalDate.now(), null, null));
    return cycles;
  }

  private static List<TrainingCycle> trainingCycles() {
    TrainingCycle cycle1 = TrainingCycle.withInstructions(Instructions.createBasicInstructions(LocalDate.now()));
    cycle1.addEntry(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker());
    cycle1.addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker());
    cycle1.addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker());

    TrainingCycle cycle2 = TrainingCycle.withInstructions(Instructions.createBasicInstructions(LocalDate.now()));
    cycle2.addEntry(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker());
    cycle2.addEntry(TrainingEntry.forText("M"), TrainingCycle.StickerExpectations.redSticker());

    TrainingCycle cycle3 = TrainingCycle.withInstructions(Instructions.createBasicInstructions(LocalDate.now()));
    cycle3.addEntry(TrainingEntry.forText("H"), TrainingCycle.StickerExpectations.redSticker());

    return ImmutableList.of(cycle1, cycle2, cycle3);
  }
}

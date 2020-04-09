package com.bloomcyclecare.cmcc.data.repos.instructions;

import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.bloomcyclecare.cmcc.data.models.TrainingCycle;
import com.bloomcyclecare.cmcc.data.models.TrainingEntry;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;
import org.junit.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class TrainingInstructionsRepoTest {
  /*
  Single<Boolean> hasAnyAfter(LocalDate date);
  */

  @Test
  public void testGet() throws Exception {
    TrainingInstructionsRepo instructionsRepo = new TrainingInstructionsRepo(trainingCycles(), LocalDate::now);

    assertThat(instructionsRepo.get(LocalDate.now()).firstOrError().blockingGet())
        .isEqualTo(Instructions.createBasicInstructions(LocalDate.now().minusDays(1)));
  }

  @Test
  public void testGetAll() throws Exception {
    TrainingInstructionsRepo instructionsRepo = new TrainingInstructionsRepo(trainingCycles(), LocalDate::now);

    Instructions firstExpected = Instructions.createBasicInstructions(LocalDate.now().minusDays(3));
    Instructions secondExpected = Instructions.createBasicInstructions(LocalDate.now().minusDays(1));

    assertThat(instructionsRepo.getAll().blockingFirst())
        .containsExactly(firstExpected, secondExpected).inOrder();
  }

  @Test
  public void testHasAnyAfter() throws Exception {
    TrainingInstructionsRepo instructionsRepo = new TrainingInstructionsRepo(trainingCycles(), LocalDate::now);

    assertThat(instructionsRepo.hasAnyAfter(LocalDate.now().minusDays(1)).blockingGet())
        .isEqualTo(false);
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

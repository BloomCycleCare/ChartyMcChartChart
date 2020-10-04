package com.bloomcyclecare.cmcc.data.models.instructions;

import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class InstructionsTest {

  @Test
  public void testDiff_basicInstructions() {
    Instructions current = new Instructions(
        LocalDate.now(), ImmutableList.of(BasicInstruction.A, BasicInstruction.B), ImmutableList.of(), ImmutableList.of());
    Instructions updated = new Instructions(
        LocalDate.now(), ImmutableList.of(BasicInstruction.B, BasicInstruction.C), ImmutableList.of(), ImmutableList.of());

    Instructions.DiffResult result = current.diff(updated);

    assertThat(result.instructionsAdded).containsExactly(BasicInstruction.C);
    assertThat(result.instructionsRemoved).containsExactly(BasicInstruction.A);
    assertThat(result.instructionsKept).containsExactly(BasicInstruction.B);
  }

  @Test
  public void testDiff_yellowStampInstructions() {
    Instructions current = new Instructions(
        LocalDate.now(), ImmutableList.of(), ImmutableList.of(), ImmutableList.of(YellowStampInstruction.YS_1_A, YellowStampInstruction.YS_1_B));
    Instructions updated = new Instructions(
        LocalDate.now(), ImmutableList.of(), ImmutableList.of(), ImmutableList.of(YellowStampInstruction.YS_1_B, YellowStampInstruction.YS_1_C));

    Instructions.DiffResult result = current.diff(updated);

    assertThat(result.instructionsAdded).containsExactly(YellowStampInstruction.YS_1_C);
    assertThat(result.instructionsRemoved).containsExactly(YellowStampInstruction.YS_1_A);
    assertThat(result.instructionsKept).containsExactly(YellowStampInstruction.YS_1_B);
  }

}
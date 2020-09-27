package com.bloomcyclecare.cmcc.data.serialization;

import com.bloomcyclecare.cmcc.data.models.instructions.Instructions;
import com.bloomcyclecare.cmcc.data.models.instructions.SpecialInstruction;
import com.bloomcyclecare.cmcc.data.models.instructions.YellowStampInstruction;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

public class InstructionsSerializerTest {

  @Test
  public void basic() {
    Instructions in = Instructions.createBasicInstructions(LocalDate.now());
    String encoded = InstructionsSerializer.encode(in, true);
    assertThat(encoded).hasLength(19);

    Instructions decoded = InstructionsSerializer.decode(encoded);
    assertEquals(in, decoded);
  }

  @Test
  public void basicWithSpecial() {
    Instructions in = Instructions.createBasicInstructions(LocalDate.now());
    in = new Instructions(in.startDate, in.activeItems,
        ImmutableList.of(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS),
        in.yellowStampInstructions);
    String encoded = InstructionsSerializer.encode(in, true);
    assertThat(encoded).hasLength(19);

    Instructions decoded = InstructionsSerializer.decode(encoded);
    assertEquals(in, decoded);
  }

  @Test
  public void basicWithYellow() {
    Instructions in = Instructions.createBasicInstructions(LocalDate.now());
    in = new Instructions(in.startDate, in.activeItems,
        in.specialInstructions,
        ImmutableList.of(YellowStampInstruction.YS_1_A, YellowStampInstruction.YS_2_A, YellowStampInstruction.YS_3_A));
    String encoded = InstructionsSerializer.encode(in, true);
    assertThat(encoded).hasLength(19);

    Instructions decoded = InstructionsSerializer.decode(encoded);
    assertEquals(in, decoded);
  }

  @Test
  public void complex() {
    Instructions in = Instructions.createBasicInstructions(LocalDate.now());
    in = new Instructions(in.startDate, in.activeItems,
        ImmutableList.of(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS),
        ImmutableList.of(YellowStampInstruction.YS_1_A, YellowStampInstruction.YS_2_A, YellowStampInstruction.YS_3_A));
    String encoded = InstructionsSerializer.encode(in, true);
    assertThat(encoded).hasLength(19);

    Instructions decoded = InstructionsSerializer.decode(encoded);
    assertEquals(in, decoded);
  }
}
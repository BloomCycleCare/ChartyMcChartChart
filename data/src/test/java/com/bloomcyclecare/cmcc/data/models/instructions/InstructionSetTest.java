package com.bloomcyclecare.cmcc.data.models.instructions;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.parceler.Parcels;

import static com.google.common.truth.Truth.assertThat;

public class InstructionSetTest {

  @Test
  public void testAdd() {
    InstructionSet set = new InstructionSet();
    set.add(BasicInstruction.A);
    set.add(YellowStampInstruction.YS_1_A);
    set.add(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS);

    assertThat(set).containsExactly(
        BasicInstruction.A,
        YellowStampInstruction.YS_1_A,
        SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS);
  }

  @Test
  public void testAddAll() {
    InstructionSet set = new InstructionSet();
    set.addAll(ImmutableList.of(
        BasicInstruction.A,
        YellowStampInstruction.YS_1_A,
        SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS));

    assertThat(set).containsExactly(
        BasicInstruction.A,
        YellowStampInstruction.YS_1_A,
        SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS);
  }

  @Test
  public void testRemove() {
    InstructionSet set = new InstructionSet();
    set.addAll(ImmutableList.of(
        BasicInstruction.A,
        YellowStampInstruction.YS_1_A,
        SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS));
    assertThat(set).containsExactly(
        BasicInstruction.A,
        YellowStampInstruction.YS_1_A,
        SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS);

    set.remove(BasicInstruction.A);
    assertThat(set).containsExactly(
        YellowStampInstruction.YS_1_A,
        SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS);

    set.remove(YellowStampInstruction.YS_1_A);
    assertThat(set).containsExactly(
        SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS);

    set.remove(SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS);
    assertThat(set).isEmpty();
  }

  @Test
  public void testParcelable() {
    InstructionSet set = InstructionSet.of(
        BasicInstruction.A,
        YellowStampInstruction.YS_1_A,
        SpecialInstruction.BREASTFEEDING_SEMINAL_FLUID_YELLOW_STAMPS);

    InstructionSet out = Parcels.unwrap(Parcels.wrap(set));
    assertThat(out).containsExactlyElementsIn(set);
  }
}

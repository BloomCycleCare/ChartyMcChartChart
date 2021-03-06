package com.bloomcyclecare.cmcc.logic.chart;

import com.bloomcyclecare.cmcc.data.models.instructions.BasicInstruction;
import com.bloomcyclecare.cmcc.data.models.instructions.InstructionSet;
import com.bloomcyclecare.cmcc.data.models.stickering.Sticker;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

public class StickerSelectorTest {

  static CycleRenderer.StickerSelectionContext RED_CONTEXT = new CycleRenderer.StickerSelectionContext(
      -1,
      null,
      true,
      true,
      true,
      false,
      true,
      true,
      InstructionSet.of(BasicInstruction.D_1),
      InstructionSet.of());

  @Test
  public void testCheck_expectRed() {
    StickerSelector.CheckResult checkResult;

    checkResult = StickerSelector.check(Sticker.RED, RED_CONTEXT);
    assertThat(checkResult.ok()).isTrue();
    assertThat(checkResult.errorMessage).isEmpty();

    checkResult = StickerSelector.check(Sticker.GREEN, RED_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains(StickerSelector.FLOW_POSITIVE_REASON);
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FLOW_POSITIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.GREEN_BABY, RED_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today has bleeding");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.BLEEDING_POSITIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.WHITE_BABY, RED_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today has bleeding");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.BLEEDING_POSITIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.YELLOW, RED_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains(StickerSelector.FLOW_POSITIVE_REASON);
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FLOW_POSITIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.YELLOW_BABY, RED_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today has bleeding");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.BLEEDING_POSITIVE_EXPLANATION);
  }

  @Test
  public void testSelect_expectRed() {
    StickerSelector.SelectResult selectResult = StickerSelector.select(RED_CONTEXT);
    assertThat(selectResult.sticker).isEqualTo(Sticker.RED);
    assertThat(selectResult.matchedCriteria).containsExactly(StickerSelector.FLOW_POSITIVE_REASON, StickerSelector.BLEEDING_POSITIVE_REASON).inOrder();
  }

  static CycleRenderer.StickerSelectionContext GREEN_CONTEXT = new CycleRenderer.StickerSelectionContext(
      -1,
      null,
      true,
      true,
      false,
      false,
      false,
      true,
      InstructionSet.of(),
      InstructionSet.of(BasicInstruction.E_1));

  @Test
  public void testCheck_expectGreen() {
    StickerSelector.CheckResult checkResult;

    checkResult = StickerSelector.check(Sticker.GREEN, GREEN_CONTEXT);
    assertThat(checkResult.ok()).isTrue();
    assertThat(checkResult.errorMessage).isEmpty();

    checkResult = StickerSelector.check(Sticker.GREEN_BABY, GREEN_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains(
        nor(StickerSelector.FLOW_POSITIVE_REASON, StickerSelector.FERTILE_POSITIVE_REASON));
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FERTILE_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.WHITE_BABY, GREEN_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains(
        nor(StickerSelector.FLOW_POSITIVE_REASON, StickerSelector.FERTILE_POSITIVE_REASON));
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FERTILE_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.YELLOW, GREEN_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have mucus");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.MUCUS_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.YELLOW_BABY, GREEN_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains(
        nor(StickerSelector.FLOW_POSITIVE_REASON, StickerSelector.FERTILE_POSITIVE_REASON));
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FERTILE_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.RED, GREEN_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains(
        nor(StickerSelector.FLOW_POSITIVE_REASON, StickerSelector.FERTILE_POSITIVE_REASON));
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FERTILE_NEGATIVE_EXPLANATION);
  }

  @Test
  public void testSelect_expectGreen() {
    StickerSelector.SelectResult selectResult = StickerSelector.select(GREEN_CONTEXT);
    assertThat(selectResult.sticker).isEqualTo(Sticker.GREEN);
    assertThat(selectResult.matchedCriteria).containsExactly(
        nor(StickerSelector.FLOW_POSITIVE_REASON, StickerSelector.FERTILE_POSITIVE_REASON),
        StickerSelector.MUCUS_NEGATIVE_REASON).inOrder();
  }

  static CycleRenderer.StickerSelectionContext GREEN_BABY_CONTEXT = new CycleRenderer.StickerSelectionContext(
      -1,
      null,
      true,
      true,
      false,
      false,
      false,
      true,
      InstructionSet.of(BasicInstruction.D_5),
      InstructionSet.of());

  @Test
  public void testCheck_expectGreenBaby() {
    StickerSelector.CheckResult checkResult;

    checkResult = StickerSelector.check(Sticker.GREEN_BABY, GREEN_BABY_CONTEXT);
    assertThat(checkResult.ok()).isTrue();
    assertThat(checkResult.errorMessage).isEmpty();

    checkResult = StickerSelector.check(Sticker.GREEN, GREEN_BABY_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains(StickerSelector.FERTILE_POSITIVE_REASON);
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FERTILE_POSITIVE_EXPLANATION);
    assertThat(checkResult.errorExplanation.get()).contains(BasicInstruction.D_5.description());

    checkResult = StickerSelector.check(Sticker.WHITE_BABY, GREEN_BABY_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have mucus");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.MUCUS_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.YELLOW, GREEN_BABY_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains(StickerSelector.FERTILE_POSITIVE_REASON);
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FERTILE_POSITIVE_EXPLANATION);
    assertThat(checkResult.errorExplanation.get()).contains(BasicInstruction.D_5.description());

    checkResult = StickerSelector.check(Sticker.YELLOW_BABY, GREEN_BABY_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have mucus");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.MUCUS_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.RED, GREEN_BABY_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have bleeding");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.BLEEDING_NEGATIVE_EXPLANATION);
  }

  @Test
  public void testSelect_expectGreenBaby() {
    StickerSelector.SelectResult selectResult = StickerSelector.select(GREEN_BABY_CONTEXT);
    assertThat(selectResult.sticker).isEqualTo(Sticker.GREEN_BABY);
    assertThat(selectResult.matchedCriteria).containsExactly(
        "doesn't have bleeding", "doesn't have mucus", "is fertile");
  }

  CycleRenderer.StickerSelectionContext WHITE_BABY_CONTEXT = new CycleRenderer.StickerSelectionContext(
      -1,
      null,
      true,
      true,
      false,
      true,
      false,
      true,
      InstructionSet.of(BasicInstruction.D_5),
      InstructionSet.of());

  @Test
  public void testCheck_expectWhiteBaby() {
    StickerSelector.CheckResult checkResult;

    checkResult = StickerSelector.check(Sticker.WHITE_BABY, WHITE_BABY_CONTEXT);
    assertThat(checkResult.ok()).isTrue();
    assertThat(checkResult.errorMessage).isEmpty();

    verifyBecauseIsFertile(StickerSelector.check(Sticker.GREEN, WHITE_BABY_CONTEXT));

    checkResult = StickerSelector.check(Sticker.GREEN_BABY, WHITE_BABY_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today has mucus");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.MUCUS_POSITIVE_EXPLANATION);

    verifyBecauseIsFertile(StickerSelector.check(Sticker.YELLOW, WHITE_BABY_CONTEXT));

    checkResult = StickerSelector.check(Sticker.YELLOW_BABY, WHITE_BABY_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have active special instructions");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.INFERTILE_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.RED, WHITE_BABY_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have bleeding");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.BLEEDING_NEGATIVE_EXPLANATION);
  }

  @Test
  public void testSelect_expectWhiteBaby() {
    StickerSelector.SelectResult selectResult = StickerSelector.select(WHITE_BABY_CONTEXT);
    assertThat(selectResult.sticker).isEqualTo(Sticker.WHITE_BABY);
    assertThat(selectResult.matchedCriteria).containsExactly(
        StickerSelector.FERTILE_POSITIVE_REASON, StickerSelector.BLEEDING_NEGATIVE_REASON, StickerSelector.MUCUS_POSITIVE_REASON, StickerSelector.INFERTILE_NEGATIVE_REASON).inOrder();
  }

  static CycleRenderer.StickerSelectionContext YELLOW_CONTEXT = new CycleRenderer.StickerSelectionContext(
      -1,
      null,
      true,
      true,
      false,
      true,
      false,
      true,
      InstructionSet.of(),
      InstructionSet.of(BasicInstruction.K_1));

  @Test
  public void testCheck_expectYellow() {
    StickerSelector.CheckResult checkResult;

    checkResult = StickerSelector.check(Sticker.YELLOW, YELLOW_CONTEXT);
    assertThat(checkResult.ok()).isTrue();
    assertThat(checkResult.errorMessage).isEmpty();

    checkResult = StickerSelector.check(Sticker.GREEN, YELLOW_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today has mucus");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.MUCUS_POSITIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.GREEN_BABY, YELLOW_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains(
        nor(StickerSelector.FLOW_POSITIVE_REASON, StickerSelector.FERTILE_POSITIVE_REASON));
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FERTILE_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.WHITE_BABY, YELLOW_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains(
        nor(StickerSelector.FLOW_POSITIVE_REASON, StickerSelector.FERTILE_POSITIVE_REASON));
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FERTILE_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.YELLOW_BABY, YELLOW_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains(
        nor(StickerSelector.FLOW_POSITIVE_REASON, StickerSelector.FERTILE_POSITIVE_REASON));
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FERTILE_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.RED, YELLOW_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains(
        nor(StickerSelector.FLOW_POSITIVE_REASON, StickerSelector.FERTILE_POSITIVE_REASON));
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FERTILE_NEGATIVE_EXPLANATION);
  }

  @Test
  public void testSelect_expectYellow() {
    StickerSelector.SelectResult selectResult = StickerSelector.select(YELLOW_CONTEXT);
    assertThat(selectResult.sticker).isEqualTo(Sticker.YELLOW);
    assertThat(selectResult.matchedCriteria).containsExactly(
        nor(StickerSelector.FLOW_POSITIVE_REASON, StickerSelector.FERTILE_POSITIVE_REASON),
        StickerSelector.MUCUS_POSITIVE_REASON).inOrder();
  }

  CycleRenderer.StickerSelectionContext YELLOW_BABY_CONTEXT = new CycleRenderer.StickerSelectionContext(
      -1,
      null,
      true,
      true,
      false,
      true,
      false,
      true,
      InstructionSet.of(BasicInstruction.D_5),
      InstructionSet.of(BasicInstruction.K_1));

  @Test
  public void testCheck_expectYellowBaby() {
    StickerSelector.CheckResult checkResult;

    checkResult = StickerSelector.check(Sticker.YELLOW_BABY, YELLOW_BABY_CONTEXT);
    assertThat(checkResult.ok()).isTrue();
    assertThat(checkResult.errorMessage).isEmpty();

    verifyBecauseIsFertile(StickerSelector.check(Sticker.GREEN, YELLOW_BABY_CONTEXT));

    checkResult = StickerSelector.check(Sticker.GREEN_BABY, YELLOW_BABY_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today has mucus");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.MUCUS_POSITIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.WHITE_BABY, YELLOW_BABY_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today has active special instructions");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.INFERTILE_POSITIVE_EXPLANATION);
    assertThat(checkResult.errorExplanation.get()).contains(BasicInstruction.K_1.description());

    verifyBecauseIsFertile(StickerSelector.check(Sticker.YELLOW, YELLOW_BABY_CONTEXT));

    checkResult = StickerSelector.check(Sticker.RED, YELLOW_BABY_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have bleeding");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.BLEEDING_NEGATIVE_EXPLANATION);
  }

  @Test
  public void testSelect_expectYellowBaby() {
    StickerSelector.SelectResult selectResult = StickerSelector.select(YELLOW_BABY_CONTEXT);
    assertThat(selectResult.sticker).isEqualTo(Sticker.YELLOW_BABY);
    assertThat(selectResult.matchedCriteria).containsExactly(
        "doesn't have bleeding", "has mucus", "has active special instructions", "is fertile");
  }

  @Test
  public void testSelect_expectGrey() {
    StickerSelector.SelectResult selectResult;

    CycleRenderer.StickerSelectionContext missingInstructions = new CycleRenderer.StickerSelectionContext(
        -1,
        null,
        false,
        true,
        false,
        false,
        false,
        true,
        InstructionSet.of(),
        InstructionSet.of());
    selectResult = StickerSelector.select(missingInstructions);
    assertThat(selectResult.sticker).isEqualTo(Sticker.GREY);
    assertThat(selectResult.matchedCriteria).containsExactly("doesn't have instructions");

    CycleRenderer.StickerSelectionContext missingObservation = new CycleRenderer.StickerSelectionContext(
        -1,
        null,
        true,
        false,
        false,
        false,
        false,
        true,
        InstructionSet.of(),
        InstructionSet.of());
    selectResult = StickerSelector.select(missingObservation);
    assertThat(selectResult.sticker).isEqualTo(Sticker.GREY);
    assertThat(selectResult.matchedCriteria).containsExactly("doesn't have observation");

    CycleRenderer.StickerSelectionContext missingBoth = new CycleRenderer.StickerSelectionContext(
        -1,
        null,
        false,
        false,
        false,
        false,
        false,
        true,
        InstructionSet.of(),
        InstructionSet.of());
    selectResult = StickerSelector.select(missingBoth);
    assertThat(selectResult.sticker).isEqualTo(Sticker.GREY);
    assertThat(selectResult.matchedCriteria).containsExactly("doesn't have observation AND doesn't have instructions");
  }

  private static void verifyBecauseIsFertile(StickerSelector.CheckResult checkResult) {
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains(StickerSelector.FERTILE_POSITIVE_REASON);
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FERTILE_POSITIVE_EXPLANATION);
    assertThat(checkResult.errorExplanation.get()).contains(BasicInstruction.D_5.description());
  }

  private static String nor(String a, String b) {
    return String.format("Neither %s NOR %s", a, b);
  }

}

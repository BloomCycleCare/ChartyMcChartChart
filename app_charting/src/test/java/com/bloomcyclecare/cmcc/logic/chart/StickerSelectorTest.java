package com.bloomcyclecare.cmcc.logic.chart;

import com.bloomcyclecare.cmcc.data.models.instructions.BasicInstruction;
import com.bloomcyclecare.cmcc.data.models.stickering.Sticker;
import com.google.common.collect.ImmutableSet;

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
      ImmutableSet.of(),
      ImmutableSet.of());

  @Test
  public void testCheck_expectRed() {
    StickerSelector.CheckResult checkResult;

    checkResult = StickerSelector.check(Sticker.RED, RED_CONTEXT);
    assertThat(checkResult.ok()).isTrue();
    assertThat(checkResult.errorMessage).isEmpty();

    checkResult = StickerSelector.check(Sticker.GREEN, RED_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today has bleeding");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.BLEEDING_POSITIVE_EXPLANATION);

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
    assertThat(checkResult.errorMessage.get()).contains("today has bleeding");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.BLEEDING_POSITIVE_EXPLANATION);

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
    assertThat(selectResult.matchedCriteria).containsExactly("has bleeding");
  }

  static CycleRenderer.StickerSelectionContext GREEN_CONTEXT = new CycleRenderer.StickerSelectionContext(
      -1,
      null,
      true,
      true,
      false,
      false,
      ImmutableSet.of(),
      ImmutableSet.of());

  @Test
  public void testCheck_expectGreen() {
    StickerSelector.CheckResult checkResult;

    checkResult = StickerSelector.check(Sticker.GREEN, GREEN_CONTEXT);
    assertThat(checkResult.ok()).isTrue();
    assertThat(checkResult.errorMessage).isEmpty();

    checkResult = StickerSelector.check(Sticker.GREEN_BABY, GREEN_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today isn't fertile");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.FERTILE_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.WHITE_BABY, GREEN_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have mucus");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.MUCUS_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.YELLOW, GREEN_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have mucus");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.MUCUS_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.YELLOW_BABY, GREEN_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have mucus");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.MUCUS_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.RED, GREEN_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have bleeding");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.BLEEDING_NEGATIVE_EXPLANATION);
  }

  @Test
  public void testSelect_expectGreen() {
    StickerSelector.SelectResult selectResult = StickerSelector.select(GREEN_CONTEXT);
    assertThat(selectResult.sticker).isEqualTo(Sticker.GREEN);
    assertThat(selectResult.matchedCriteria).containsExactly(
        "doesn't have bleeding", "doesn't have mucus", "isn't fertile");
  }

  static CycleRenderer.StickerSelectionContext GREEN_BABY_CONTEXT = new CycleRenderer.StickerSelectionContext(
      -1,
      null,
      true,
      true,
      false,
      false,
      ImmutableSet.of(BasicInstruction.D_5),
      ImmutableSet.of());

  @Test
  public void testCheck_expectGreenBaby() {
    StickerSelector.CheckResult checkResult;

    checkResult = StickerSelector.check(Sticker.GREEN_BABY, GREEN_BABY_CONTEXT);
    assertThat(checkResult.ok()).isTrue();
    assertThat(checkResult.errorMessage).isEmpty();

    checkResult = StickerSelector.check(Sticker.GREEN, GREEN_BABY_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today is fertile");
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
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have mucus");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.MUCUS_NEGATIVE_EXPLANATION);

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
      ImmutableSet.of(),
      ImmutableSet.of());

  @Test
  public void testCheck_expectWhiteBaby() {
    StickerSelector.CheckResult checkResult;

    checkResult = StickerSelector.check(Sticker.WHITE_BABY, WHITE_BABY_CONTEXT);
    assertThat(checkResult.ok()).isTrue();
    assertThat(checkResult.errorMessage).isEmpty();

    checkResult = StickerSelector.check(Sticker.GREEN, WHITE_BABY_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today has mucus");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.MUCUS_POSITIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.GREEN_BABY, WHITE_BABY_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today has mucus");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.MUCUS_POSITIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.YELLOW, WHITE_BABY_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have active special instructions");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.INFERTILE_NEGATIVE_EXPLANATION);

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
        "doesn't have bleeding", "has mucus", "doesn't have active special instructions");
  }

  static CycleRenderer.StickerSelectionContext YELLOW_CONTEXT = new CycleRenderer.StickerSelectionContext(
      -1,
      null,
      true,
      true,
      false,
      true,
      ImmutableSet.of(),
      ImmutableSet.of(BasicInstruction.K_1));

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
    assertThat(checkResult.errorMessage.get()).contains("today has mucus");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.MUCUS_POSITIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.WHITE_BABY, YELLOW_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today has active special instructions");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.INFERTILE_POSITIVE_EXPLANATION);
    assertThat(checkResult.errorExplanation.get()).contains(BasicInstruction.K_1.description());

    checkResult = StickerSelector.check(Sticker.YELLOW_BABY, YELLOW_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today isn't fertile");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.FERTILE_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.RED, YELLOW_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have bleeding");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.BLEEDING_NEGATIVE_EXPLANATION);
  }

  @Test
  public void testSelect_expectYellow() {
    StickerSelector.SelectResult selectResult = StickerSelector.select(YELLOW_CONTEXT);
    assertThat(selectResult.sticker).isEqualTo(Sticker.YELLOW);
    assertThat(selectResult.matchedCriteria).containsExactly(
        "doesn't have bleeding", "has mucus", "has active special instructions", "isn't fertile");
  }

  CycleRenderer.StickerSelectionContext YELLOW_BABY_CONTEXT = new CycleRenderer.StickerSelectionContext(
      -1,
      null,
      true,
      true,
      false,
      true,
      ImmutableSet.of(BasicInstruction.D_5),
      ImmutableSet.of(BasicInstruction.K_1));

  @Test
  public void testCheck_expectYellowBaby() {
    StickerSelector.CheckResult checkResult;

    checkResult = StickerSelector.check(Sticker.YELLOW_BABY, YELLOW_BABY_CONTEXT);
    assertThat(checkResult.ok()).isTrue();
    assertThat(checkResult.errorMessage).isEmpty();

    checkResult = StickerSelector.check(Sticker.GREEN, YELLOW_BABY_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today has mucus");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.MUCUS_POSITIVE_EXPLANATION);

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

    checkResult = StickerSelector.check(Sticker.YELLOW, YELLOW_BABY_CONTEXT);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today is fertile");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FERTILE_POSITIVE_EXPLANATION);
    assertThat(checkResult.errorExplanation.get()).contains(BasicInstruction.D_5.description());

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
}

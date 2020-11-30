package com.bloomcyclecare.cmcc.logic.chart;

import com.bloomcyclecare.cmcc.data.models.instructions.BasicInstruction;
import com.bloomcyclecare.cmcc.data.models.stickering.Sticker;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

public class StickerSelectorTest {

  @Test
  public void testExpectRed() {
    CycleRenderer.StickerSelectionContext context = new CycleRenderer.StickerSelectionContext(
        -1,
        null,
        true,
        true,
        true,
        false,
        ImmutableSet.of(BasicInstruction.D_1),
        ImmutableSet.of());
    StickerSelector.CheckResult checkResult;

    checkResult = StickerSelector.check(Sticker.RED, context);
    assertThat(checkResult.ok()).isTrue();
    assertThat(checkResult.errorMessage).isEmpty();

    checkResult = StickerSelector.check(Sticker.GREEN, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today is fertile");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FERTILE_POSITIVE_EXPLANATION);
    assertThat(checkResult.errorExplanation.get()).contains(BasicInstruction.D_1.description());

    checkResult = StickerSelector.check(Sticker.GREEN_BABY, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today has bleeding");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.BLEEDING_POSITIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.WHITE_BABY, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today has bleeding");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.BLEEDING_POSITIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.YELLOW, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today is fertile");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FERTILE_POSITIVE_EXPLANATION);
    assertThat(checkResult.errorExplanation.get()).contains(BasicInstruction.D_1.description());

    checkResult = StickerSelector.check(Sticker.YELLOW_BABY, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today has bleeding");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.BLEEDING_POSITIVE_EXPLANATION);

    StickerSelector.SelectResult selectResult = StickerSelector.select(context);
    assertThat(selectResult.sticker).isEqualTo(Sticker.RED);
    assertThat(selectResult.matchedCriteria).containsExactly(
        "is fertile", "has bleeding");
  }

  @Test
  public void testExpectGreen() {
    CycleRenderer.StickerSelectionContext context = new CycleRenderer.StickerSelectionContext(
        -1,
        null,
        true,
        true,
        false,
        false,
        ImmutableSet.of(),
        ImmutableSet.of());
    StickerSelector.CheckResult checkResult;

    checkResult = StickerSelector.check(Sticker.GREEN, context);
    assertThat(checkResult.ok()).isTrue();
    assertThat(checkResult.errorMessage).isEmpty();

    checkResult = StickerSelector.check(Sticker.GREEN_BABY, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today isn't fertile");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.FERTILE_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.WHITE_BABY, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today isn't fertile");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.FERTILE_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.YELLOW, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have mucus");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.MUCUS_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.YELLOW_BABY, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today isn't fertile");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.FERTILE_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.RED, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today isn't fertile");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.FERTILE_NEGATIVE_EXPLANATION);

    StickerSelector.SelectResult selectResult = StickerSelector.select(context);
    assertThat(selectResult.sticker).isEqualTo(Sticker.GREEN);
    assertThat(selectResult.matchedCriteria).containsExactly(
        "doesn't have mucus", "isn't fertile");
  }

  @Test
  public void testExpectGreenBaby() {
    CycleRenderer.StickerSelectionContext context = new CycleRenderer.StickerSelectionContext(
        -1,
        null,
        true,
        true,
        false,
        false,
        ImmutableSet.of(BasicInstruction.D_5),
        ImmutableSet.of());
    StickerSelector.CheckResult checkResult;

    checkResult = StickerSelector.check(Sticker.GREEN_BABY, context);
    assertThat(checkResult.ok()).isTrue();
    assertThat(checkResult.errorMessage).isEmpty();

    checkResult = StickerSelector.check(Sticker.GREEN, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today is fertile");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FERTILE_POSITIVE_EXPLANATION);
    assertThat(checkResult.errorExplanation.get()).contains(BasicInstruction.D_5.description());

    checkResult = StickerSelector.check(Sticker.WHITE_BABY, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have mucus");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.MUCUS_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.YELLOW, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today is fertile");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FERTILE_POSITIVE_EXPLANATION);
    assertThat(checkResult.errorExplanation.get()).contains(BasicInstruction.D_5.description());

    checkResult = StickerSelector.check(Sticker.YELLOW_BABY, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have mucus");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.MUCUS_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.RED, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have bleeding");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.BLEEDING_NEGATIVE_EXPLANATION);

    StickerSelector.SelectResult selectResult = StickerSelector.select(context);
    assertThat(selectResult.sticker).isEqualTo(Sticker.GREEN_BABY);
    assertThat(selectResult.matchedCriteria).containsExactly(
        "doesn't have bleeding", "doesn't have mucus", "is fertile");
  }

  @Test
  public void testExpectWhiteBaby() {
    CycleRenderer.StickerSelectionContext context = new CycleRenderer.StickerSelectionContext(
        -1,
        null,
        true,
        true,
        false,
        true,
        ImmutableSet.of(BasicInstruction.D_5),
        ImmutableSet.of());
    StickerSelector.CheckResult checkResult;

    checkResult = StickerSelector.check(Sticker.WHITE_BABY, context);
    assertThat(checkResult.ok()).isTrue();
    assertThat(checkResult.errorMessage).isEmpty();

    checkResult = StickerSelector.check(Sticker.GREEN, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today is fertile");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FERTILE_POSITIVE_EXPLANATION);
    assertThat(checkResult.errorExplanation.get()).contains(BasicInstruction.D_5.description());

    checkResult = StickerSelector.check(Sticker.GREEN_BABY, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today has mucus");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.MUCUS_POSITIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.YELLOW, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today is fertile");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FERTILE_POSITIVE_EXPLANATION);
    assertThat(checkResult.errorExplanation.get()).contains(BasicInstruction.D_5.description());

    checkResult = StickerSelector.check(Sticker.YELLOW_BABY, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have active special instructions");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.INFERTILE_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.RED, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have bleeding");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.BLEEDING_NEGATIVE_EXPLANATION);

    StickerSelector.SelectResult selectResult = StickerSelector.select(context);
    assertThat(selectResult.sticker).isEqualTo(Sticker.WHITE_BABY);
    assertThat(selectResult.matchedCriteria).containsExactly(
        "is fertile", "doesn't have bleeding", "has mucus", "doesn't have active special instructions");
  }

  @Test
  public void testExpectYellow() {
    CycleRenderer.StickerSelectionContext context = new CycleRenderer.StickerSelectionContext(
        -1,
        null,
        true,
        true,
        false,
        true,
        ImmutableSet.of(),
        ImmutableSet.of(BasicInstruction.K_1));
    StickerSelector.CheckResult checkResult;

    checkResult = StickerSelector.check(Sticker.YELLOW, context);
    assertThat(checkResult.ok()).isTrue();
    assertThat(checkResult.errorMessage).isEmpty();

    checkResult = StickerSelector.check(Sticker.GREEN, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today has mucus");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.MUCUS_POSITIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.GREEN_BABY, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today isn't fertile");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.FERTILE_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.WHITE_BABY, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today isn't fertile");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.FERTILE_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.YELLOW_BABY, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today isn't fertile");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.FERTILE_NEGATIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.RED, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today isn't fertile");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.FERTILE_NEGATIVE_EXPLANATION);

    StickerSelector.SelectResult selectResult = StickerSelector.select(context);
    assertThat(selectResult.sticker).isEqualTo(Sticker.YELLOW);
    assertThat(selectResult.matchedCriteria).containsExactly(
        "has mucus", "isn't fertile");
  }

  @Test
  public void testExpectYellowBaby() {
    CycleRenderer.StickerSelectionContext context = new CycleRenderer.StickerSelectionContext(
        -1,
        null,
        true,
        true,
        false,
        true,
        ImmutableSet.of(BasicInstruction.D_5),
        ImmutableSet.of(BasicInstruction.K_1));
    StickerSelector.CheckResult checkResult;

    checkResult = StickerSelector.check(Sticker.YELLOW_BABY, context);
    assertThat(checkResult.ok()).isTrue();
    assertThat(checkResult.errorMessage).isEmpty();

    checkResult = StickerSelector.check(Sticker.GREEN, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today is fertile");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FERTILE_POSITIVE_EXPLANATION);
    assertThat(checkResult.errorExplanation.get()).contains(BasicInstruction.D_5.description());

    checkResult = StickerSelector.check(Sticker.GREEN_BABY, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today has mucus");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.MUCUS_POSITIVE_EXPLANATION);

    checkResult = StickerSelector.check(Sticker.WHITE_BABY, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today has active special instructions");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.INFERTILE_POSITIVE_EXPLANATION);
    assertThat(checkResult.errorExplanation.get()).contains(BasicInstruction.K_1.description());

    checkResult = StickerSelector.check(Sticker.YELLOW, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today is fertile");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).contains(StickerSelector.FERTILE_POSITIVE_EXPLANATION);
    assertThat(checkResult.errorExplanation.get()).contains(BasicInstruction.D_5.description());

    checkResult = StickerSelector.check(Sticker.RED, context);
    assertThat(checkResult.errorMessage).isPresent();
    assertThat(checkResult.errorMessage.get()).contains("today doesn't have bleeding");
    assertThat(checkResult.errorExplanation).isPresent();
    assertThat(checkResult.errorExplanation.get()).isEqualTo(StickerSelector.BLEEDING_NEGATIVE_EXPLANATION);

    StickerSelector.SelectResult selectResult = StickerSelector.select(context);
    assertThat(selectResult.sticker).isEqualTo(Sticker.YELLOW_BABY);
    assertThat(selectResult.matchedCriteria).containsExactly(
        "doesn't have bleeding", "has mucus", "has active special instructions", "is fertile");
  }
}

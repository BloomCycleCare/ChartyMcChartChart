package com.bloomcyclecare.cmcc.logic.chart;

import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelection;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerText;

import javax.annotation.Nullable;

import timber.log.Timber;

public class SelectionChecker {

  private final CycleRenderer.StickerSelectionContext context;

  public static SelectionChecker create(CycleRenderer.StickerSelectionContext context) {
    return new SelectionChecker(context);
  }

  private SelectionChecker(CycleRenderer.StickerSelectionContext context) {
    this.context = context;
  }

  public enum Reason {
    P_ONLY_ON_PEAK_DAY,
    INCORRECT_COUNT,
    TEXT_INPUT_REQUIRED
  }

  public enum Hint {
    ALWAYS_P_ON_PEAK_DAY,
    NO_TEXT_NEEDED
  }

  public static class Result {
    public final StickerSelection expected;
    public final StickerSelection selection;
    public final @Nullable String reason;
    public final @Nullable String explanation;
    public final @Nullable String hint;

    Result(StickerSelection expected, StickerSelection selection, @Nullable String reason, @Nullable String explanation, @Nullable String hint) {
      this.expected = expected;
      this.selection = selection;
      this.reason = reason;
      this.explanation = explanation;
      this.hint = hint;
    }

    public boolean ok() {
      return reason == null;
    }
  }

  public Result check(StickerSelection selection) {
    StickerSelection expected = context.expectedSelection();
    StickerSelector.CheckResult checkResult = StickerSelector.check(selection.sticker, context);
    return new Result(
        expected, selection, getReason(selection, checkResult), checkResult.errorExplanation.orElse(null), getHint(selection, checkResult));
  }

  @Nullable
  private String getReason(StickerSelection selection, StickerSelector.CheckResult checkResult) {
    if (selection.equals(context.expectedSelection())) {
      return null;
    }
    if (!checkResult.ok()) {
      if (!checkResult.errorMessage.isPresent()) {
        Timber.e("Missing error message! Expected: %s Selected %s", context.expectedSelection(), selection);
        return "ERROR";
      }
      return checkResult.errorMessage.get();
    }
    if (selection.text == null) {
      return Reason.TEXT_INPUT_REQUIRED.name();
    }
    switch (selection.text) {
      case P:
        return Reason.P_ONLY_ON_PEAK_DAY.name();
      case ONE:
      case TWO:
      case THREE:
        return Reason.INCORRECT_COUNT.name();
    }
    throw new IllegalStateException();
  }

  @Nullable
  private String getHint(StickerSelection selection, StickerSelector.CheckResult checkResult) {
    StickerSelection expected = context.expectedSelection();
    if (selection.equals(expected)) {
      return null;
    }
    if (!checkResult.ok()) {
      return checkResult.hint.orElse(null);
    }
    if (expected.text == null) {
      return Hint.NO_TEXT_NEEDED.name();
    }
    if (expected.text == StickerText.P) {
      return Hint.ALWAYS_P_ON_PEAK_DAY.name();
    }
    return null;
  }
}

package com.bloomcyclecare.cmcc.logic.training;

import com.bloomcyclecare.cmcc.data.models.StickerSelection;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.logic.chart.StickerColor;

import java.util.Optional;

public class SelectionChecker {

  public enum Reason {
    RED_ONLY_ON_DAYS_OF_BLEEDING,
    GREEN_ONLY_ON_DRY_DAY_OUTSIDE_COUNT,
    GREEN_BABY_ONLY_ON_DRY_DAY_WITHIN_COUNT,
    WHITE_BABY_ONLY_ON_DAYS_OF_MUCUS,
    P_ONLY_ON_PEAK_DAY,
    INCORRECT_COUNT,
    TEXT_INPUT_REQUIRED,
    NOT_YET_SUPPORTED
  }

  public enum Hint {
    ALWAYS_RED_ON_DAYS_OF_BLEEDING,
    ALWAYS_GREEN_BABY_ON_DRY_DAYS_WITHIN_COUNT,
    ALWAYS_GREEN_ON_DRY_DAYS_WITHOUT_COUNT,
    ALWAYS_WHITE_BABY_ON_DAYS_WITH_MUCUS,
  }

  public static class Result {
    public final StickerSelection expected;
    public final Optional<Reason> reason;
    public final Optional<Hint> hint;

    Result(StickerSelection expected, Optional<Reason> reason, Optional<Hint> hint) {
      this.expected = expected;
      this.reason = reason;
      this.hint = hint;
    }

    public boolean ok() {
      return !reason.isPresent();
    }
  }

  public static Result check(StickerSelection selection, CycleRenderer.RenderableEntry renderableEntry) {
    return check(selection, StickerSelection.fromRenderableEntry(renderableEntry));
  }
  public static Result check(StickerSelection selection, StickerSelection expected) {
    return new Result(expected, getReason(selection, expected), getHint(selection, expected));
  }

  private static Optional<Reason> getReason(StickerSelection selection, StickerSelection expected) {
    if (selection.equals(expected)) {
      return Optional.empty();
    }
    if (selection.sticker != expected.sticker) {
      if (selection.sticker == StickerSelection.Sticker.RED) {
        return Optional.of(Reason.RED_ONLY_ON_DAYS_OF_BLEEDING);
      }
      if (selection.sticker.color == StickerColor.GREEN) {
        if (selection.sticker.hasBaby && !expected.sticker.hasBaby) {
          return Optional.of(Reason.GREEN_BABY_ONLY_ON_DRY_DAY_WITHIN_COUNT);
        }
        return Optional.of(Reason.GREEN_ONLY_ON_DRY_DAY_OUTSIDE_COUNT);
      }
      if (selection.sticker == StickerSelection.Sticker.WHITE_BABY) {
        return Optional.of(Reason.WHITE_BABY_ONLY_ON_DAYS_OF_MUCUS);
      }
      if (selection.sticker.color == StickerColor.YELLOW) {
        // TODO: implement yellow stamp logic
        return Optional.of(Reason.NOT_YET_SUPPORTED);
      }
      throw new IllegalStateException();
    } else {
      if (selection.text == null) {
        return Optional.of(Reason.TEXT_INPUT_REQUIRED);
      }
      switch (selection.text) {
        case P:
          return Optional.of(Reason.P_ONLY_ON_PEAK_DAY);
        case ONE:
        case TWO:
        case THREE:
          return Optional.of(Reason.INCORRECT_COUNT);
      }
      throw new IllegalStateException();
    }
  }

  private static Optional<Hint> getHint(StickerSelection selection, StickerSelection expected) {
    if (selection.equals(expected)) {
      return Optional.empty();
    }
    if (selection.sticker != expected.sticker) {
      if (expected.sticker == StickerSelection.Sticker.RED) {
        return Optional.of(Hint.ALWAYS_RED_ON_DAYS_OF_BLEEDING);
      }
      if (expected.sticker == StickerSelection.Sticker.GREEN_BABY) {
        return Optional.of(Hint.ALWAYS_GREEN_BABY_ON_DRY_DAYS_WITHIN_COUNT);
      }
      if (expected.sticker == StickerSelection.Sticker.GREEN) {
        return Optional.of(Hint.ALWAYS_GREEN_ON_DRY_DAYS_WITHOUT_COUNT);
      }
      if (expected.sticker == StickerSelection.Sticker.WHITE_BABY) {
        return Optional.of(Hint.ALWAYS_WHITE_BABY_ON_DAYS_WITH_MUCUS);
      }
      // TODO: implement yellow stamp logic
    }
    return Optional.empty();
  }
}

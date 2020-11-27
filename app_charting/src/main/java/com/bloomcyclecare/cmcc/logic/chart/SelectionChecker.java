package com.bloomcyclecare.cmcc.logic.chart;

import com.bloomcyclecare.cmcc.data.models.stickering.Sticker;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerColor;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelection;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerText;

import java.util.Optional;

public class SelectionChecker {

  private final StickerSelection expected;

  public static SelectionChecker create(StickerSelection expected) {
    return new SelectionChecker(expected);
  }

  private SelectionChecker(StickerSelection expected) {
    this.expected = expected;
  }

  public enum Reason {
    RED_ONLY_ON_DAYS_OF_BLEEDING,
    GREEN_ONLY_ON_DRY_DAY_OUTSIDE_COUNT,
    GREEN_BABY_ONLY_ON_DRY_DAY_WITHIN_COUNT,
    WHITE_BABY_ONLY_ON_DAYS_OF_MUCUS,
    P_ONLY_ON_PEAK_DAY,
    INCORRECT_COUNT,
    TEXT_INPUT_REQUIRED,
    NOT_YET_SUPPORTED,
    NO_STICKER_SELECTED,
    YELLOW_STAMPS_NOT_ENABLED
  }

  public enum Hint {
    ALWAYS_RED_ON_DAYS_OF_BLEEDING,
    ALWAYS_GREEN_BABY_ON_DRY_DAYS_WITHIN_COUNT,
    ALWAYS_GREEN_ON_DRY_DAYS_WITHOUT_COUNT,
    ALWAYS_WHITE_BABY_ON_DAYS_WITH_MUCUS,
    ALWAYS_P_ON_PEAK_DAY,
    NO_TEXT_NEEDED,
    NEVER_YELLOW_STAMPS_UNLESS_ENABLED
  }

  public static class Result {
    public final StickerSelection expected;
    public final StickerSelection selection;
    public final Optional<Reason> reason;
    public final Optional<Hint> hint;

    Result(StickerSelection expected, StickerSelection selection, Optional<Reason> reason, Optional<Hint> hint) {
      this.expected = expected;
      this.selection = selection;
      this.reason = reason;
      this.hint = hint;
    }

    public boolean ok() {
      return !reason.isPresent();
    }

    @Override
    public String toString() {
      return String.format("Reason: %s Hint: %s", reason, hint);
    }
  }

  public Result check(StickerSelection selection) {
    return new Result(expected, selection, getReason(selection), getHint(selection));
  }

  private Optional<Reason> getReason(StickerSelection selection) {
    if (selection.equals(expected)) {
      return Optional.empty();
    }
    if (selection.sticker != expected.sticker) {
      if (selection.sticker == Sticker.RED) {
        return Optional.of(Reason.RED_ONLY_ON_DAYS_OF_BLEEDING);
      }
      if (selection.sticker.color == StickerColor.GREEN) {
        if (selection.sticker.hasBaby && !expected.sticker.hasBaby) {
          return Optional.of(Reason.GREEN_BABY_ONLY_ON_DRY_DAY_WITHIN_COUNT);
        }
        return Optional.of(Reason.GREEN_ONLY_ON_DRY_DAY_OUTSIDE_COUNT);
      }
      if (selection.sticker == Sticker.WHITE_BABY) {
        return Optional.of(Reason.WHITE_BABY_ONLY_ON_DAYS_OF_MUCUS);
      }
      if (selection.sticker.color == StickerColor.YELLOW) {
        // TODO: implement yellow stamp logic
        return Optional.of(Reason.NOT_YET_SUPPORTED);
      }
      if (selection.sticker.color == StickerColor.GREY) {
        return Optional.of(Reason.NO_STICKER_SELECTED);
      }
      throw new IllegalStateException(
          String.format("Unmatched color difference for selection %s", selection.sticker.name()));
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

  private Optional<Hint> getHint(StickerSelection selection) {
    if (selection.equals(expected)) {
      return Optional.empty();
    }
    if (selection.sticker != expected.sticker) {
      if (expected.sticker == Sticker.RED) {
        return Optional.of(Hint.ALWAYS_RED_ON_DAYS_OF_BLEEDING);
      }
      if (expected.sticker == Sticker.GREEN_BABY) {
        return Optional.of(Hint.ALWAYS_GREEN_BABY_ON_DRY_DAYS_WITHIN_COUNT);
      }
      if (expected.sticker == Sticker.GREEN) {
        return Optional.of(Hint.ALWAYS_GREEN_ON_DRY_DAYS_WITHOUT_COUNT);
      }
      if (expected.sticker == Sticker.WHITE_BABY) {
        return Optional.of(Hint.ALWAYS_WHITE_BABY_ON_DAYS_WITH_MUCUS);
      }
      // TODO: implement yellow stamp logic
    }
    if (expected.text == null) {
      return Optional.of(Hint.NO_TEXT_NEEDED);
    }
    if (expected.text == StickerText.P) {
      return Optional.of(Hint.ALWAYS_P_ON_PEAK_DAY);
    }
    return Optional.empty();
  }
}

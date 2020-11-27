package com.bloomcyclecare.cmcc.logic.chart;

import com.bloomcyclecare.cmcc.data.models.stickering.Sticker;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelection;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerText;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.SimpleSubjectBuilder;
import com.google.common.truth.Subject;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.junit.Test;

import java.util.function.BiConsumer;

import static com.google.common.truth.Fact.simpleFact;
import static com.google.common.truth.Truth.assertWithMessage;

public class SelectionCheckerTest {

  @Test
  public void expectedRed() {
    runForExpectation(StickerSelection.create(Sticker.RED, null), (result, baseAssert) -> {
      SelectionChecker.Hint expectedHint;
      if (result.selection.sticker == Sticker.RED) {
        expectedHint = SelectionChecker.Hint.NO_TEXT_NEEDED;
      } else {
        expectedHint = SelectionChecker.Hint.ALWAYS_RED_ON_DAYS_OF_BLEEDING;
      }
      baseAssert.that(result).hasHintWithValue(expectedHint);

      SelectionChecker.Reason expectedReason;
      if (result.selection.sticker == Sticker.GREEN) {

      }
    });
  }

  @Test
  public void selectedRed_reasonValid() {
    runForSelection(StickerSelection.create(Sticker.RED, null), (result, baseAssert) -> {
      SelectionChecker.Reason expectedReason;
      if (result.expected.sticker != Sticker.RED) {
        expectedReason = SelectionChecker.Reason.RED_ONLY_ON_DAYS_OF_BLEEDING;
      } else {
        expectedReason = SelectionChecker.Reason.TEXT_INPUT_REQUIRED;
      }
      baseAssert.that(result).hasReasonWithValue(expectedReason);
  });
  }

  @Test
  public void expectedGreenWithoutBaby() {
    runForExpectation(StickerSelection.create(Sticker.GREEN, null), (result, baseAssert) -> {
      SelectionChecker.Hint expectedHint;
      if (result.selection.sticker == Sticker.GREEN) {
        expectedHint = SelectionChecker.Hint.NO_TEXT_NEEDED;
      } else {
        expectedHint = SelectionChecker.Hint.ALWAYS_GREEN_ON_DRY_DAYS_WITHOUT_COUNT;
      }
      baseAssert.that(result).hasHintWithValue(expectedHint);
    });
  }

  private void runForExpectation(
      StickerSelection expected,
      BiConsumer<SelectionChecker.Result, SimpleSubjectBuilder<ResultSubject, SelectionChecker.Result>> fn) {
    SelectionChecker checker = SelectionChecker.create(expected);
    for (StickerSelection selection : getSelections()) {
      SimpleSubjectBuilder<ResultSubject, SelectionChecker.Result> baseAssert =
          assertWithMessage(String.format("Checking selection \"%s\"", selection))
              .about(ResultSubject.results());
      SelectionChecker.Result result = checker.check(selection);
      if (selection.equals(expected)) {
        baseAssert.that(result).isOk();
      } else {
        baseAssert.that(result).isNotOk();
        fn.accept(result, baseAssert);
      }
    }
  }

  private void runForSelection(
      StickerSelection selection,
      BiConsumer<SelectionChecker.Result, SimpleSubjectBuilder<ResultSubject, SelectionChecker.Result>> fn) {
    for (StickerSelection expected : getSelections()) {
      SelectionChecker checker = SelectionChecker.create(expected);
      SimpleSubjectBuilder<ResultSubject, SelectionChecker.Result> baseAssert =
          assertWithMessage(String.format("Checking expected \"%s\"", expected))
              .about(ResultSubject.results());
      SelectionChecker.Result result = checker.check(selection);
      if (selection.equals(expected)) {
        baseAssert.that(result).isOk();
      } else {
        baseAssert.that(result).isNotOk();
        fn.accept(result, baseAssert);
      }
    }
  }

  private static class ResultSubject extends Subject {

    private final SelectionChecker.Result actual;

    protected ResultSubject(FailureMetadata metadata, @NullableDecl SelectionChecker.Result actual) {
      super(metadata, actual);
      this.actual = actual;
    }

    public void isOk() {
      if (!actual.ok()) {
        failWithActual(simpleFact("expected ok result"));
      }
    }

    public void isNotOk() {
      if (actual.ok()) {
        failWithActual(simpleFact("expected NOT ok result"));
      }
    }

    public void hasHintWithValue(SelectionChecker.Hint hint) {
      if (!actual.hint.isPresent()) {
        failWithActual(simpleFact("expected to have hint but none was present"));
        return;
      }
      if (actual.hint.get() != hint) {
        failWithActual(simpleFact(String.format("expected hint %s", hint.name())));
        return;
      }
    }

    public void hasReasonWithValue(SelectionChecker.Reason reason) {
      if (!actual.reason.isPresent()) {
        failWithActual(simpleFact("expected to have reason but none was present"));
        return;
      }
      if (actual.reason.get() != reason) {
        failWithActual(simpleFact(String.format("expected reason %s", reason.name())));
        return;
      }
    }

    public static Subject.Factory<ResultSubject, SelectionChecker.Result> results() {
      return ResultSubject::new;
    }
  }

  private static ImmutableList<StickerSelection> getSelections() {
    ImmutableList.Builder<StickerSelection> out = ImmutableList.builder();
    for (Sticker sticker : Sticker.values()) {
      out.add(StickerSelection.create(sticker, null));
      for (StickerText text : StickerText.values()) {
        out.add(StickerSelection.create(sticker, text));
      }
    }
    return out.build();
  }
}

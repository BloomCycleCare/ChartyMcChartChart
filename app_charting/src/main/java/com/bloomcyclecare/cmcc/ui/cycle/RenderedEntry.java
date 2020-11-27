package com.bloomcyclecare.cmcc.ui.cycle;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.data.models.charting.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelection;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.utils.StickerUtil;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;

import org.joda.time.LocalDate;

import java.util.Optional;

import androidx.annotation.NonNull;

@AutoValue
public abstract class RenderedEntry {

  @NonNull public abstract String entryNum();
  @NonNull public abstract LocalDate entryDate();
  @NonNull public abstract String entryDateStr();
  @NonNull public abstract String entryDateShortStr();
  @NonNull public abstract Optional<String> observationSummary();
  @NonNull public abstract String stickerText();
  @NonNull public abstract String pocSummaryText();
  @NonNull public abstract String essentialSamenessSummary();
  @NonNull public abstract String instructionSummary();

  public abstract int stickerBackgroundResource();
  public abstract boolean showStickerStrike();
  public abstract boolean showWeekTransition();

  public abstract boolean canNavigateToDetailActivity();
  public abstract boolean canPromptForStickerSelection();
  public abstract boolean canSelectYellowStamps();
  public abstract boolean hasObservation();

  @NonNull public abstract CycleRenderer.EntryModificationContext entryModificationContext();
  @NonNull public abstract Optional<StickerSelection> expectedStickerSelection();
  @NonNull public abstract Optional<StickerSelection> manualStickerSelection();

  public static RenderedEntry create(String entryNum, LocalDate entryDate, String entryDateStr, String entryDateShortStr, Optional<String> observationSummary, String stickerText, String pocSummaryText, String essentialSamenessSummary, String instructionSummary, int stickerBackgroundResource, boolean showStickerStrike, boolean showWeekTransition, boolean canNavigateToDetailActivity, boolean canPromptForStickerSelection, boolean canSelectYellowStamps, boolean hasObservation, CycleRenderer.EntryModificationContext entryModificationContext, Optional<StickerSelection> expectedStickerSelection, Optional<StickerSelection> manualStickerSelection) {
    return new AutoValue_RenderedEntry(entryNum, entryDate, entryDateStr, entryDateShortStr, observationSummary, stickerText, pocSummaryText, essentialSamenessSummary, instructionSummary, stickerBackgroundResource, showStickerStrike, showWeekTransition, canNavigateToDetailActivity, canPromptForStickerSelection, canSelectYellowStamps, hasObservation, entryModificationContext, expectedStickerSelection, manualStickerSelection);
  }

  @NonNull
  public static RenderedEntry create(@NonNull CycleRenderer.RenderableEntry re,
                                     boolean autoStickeringEnabled,
                                     @NonNull ViewMode viewMode) {
    StickerSelection manualSelection = re.manualStickerSelection().orElse(null);
    boolean hasNonEmptyManualSelection = manualSelection != null && !manualSelection.equals(StickerSelection.empty());
    Optional<StickerSelection> expectedStickerSelection = re.hasObservation()
        ? Optional.of(StickerSelection.create(re.expectedStickerSelection().sticker,
        viewMode == ViewMode.TRAINING ? null : re.expectedStickerSelection().text))
        : Optional.empty();
    Optional<StickerSelection> manualStickerSelection = hasNonEmptyManualSelection
        ? re.manualStickerSelection() : Optional.empty();
    StickerSelection autoSelection = expectedStickerSelection.orElse(StickerSelection.empty());

    int resourceId = R.drawable.sticker_grey;
    if (hasNonEmptyManualSelection) {
      resourceId = StickerUtil.resourceId(manualSelection.sticker);
    } else if (viewMode != ViewMode.TRAINING && (autoStickeringEnabled || viewMode == ViewMode.DEMO)) {
      resourceId = StickerUtil.resourceId(autoSelection.sticker);
    }

    String stickerText = "";
    if (viewMode == ViewMode.TRAINING) {
      stickerText = re.trainingMarker();
    } else if (autoStickeringEnabled || viewMode == ViewMode.DEMO) {
      stickerText = autoSelection.text != null
          ? String.valueOf(autoSelection.text.value) : "";
    } else if (hasNonEmptyManualSelection) {
      stickerText = manualSelection.text != null
          ? String.valueOf(manualSelection.text.value) : "";
    } else {
      stickerText = "?";
    }

    String pocSummary = viewMode == ViewMode.TRAINING ? "" : re.pocSummary();

    ChartEntry entry = re.modificationContext().entry;
    boolean showStickerStrike = !autoStickeringEnabled && hasNonEmptyManualSelection && !autoSelection.equals(manualSelection);
    boolean showWeekTransition = entry.observationEntry.getDate().dayOfWeek().getAsString().equals("1");

    boolean canNavigateToDetailActivity = viewMode == ViewMode.CHARTING || (
        viewMode == ViewMode.TRAINING && !Strings.isNullOrEmpty(re.trainingMarker()));
    boolean canPromptForStickerSelection =
        (viewMode == ViewMode.CHARTING && !autoStickeringEnabled)
        || (viewMode == ViewMode.TRAINING && !Strings.isNullOrEmpty(re.trainingMarker()));

    return create(
        String.valueOf(re.entryNum()),
        re.modificationContext().entry.entryDate,
        re.dateSummary(),
        re.dateSummaryShort(),
        re.entrySummary(),
        stickerText,
        pocSummary,
        re.essentialSamenessSummary(),
        re.instructionSummary(),
        resourceId,
        showStickerStrike,
        showWeekTransition,
        canNavigateToDetailActivity,
        canPromptForStickerSelection,
        re.canSelectYellowStamps(),
        entry.hasObservation(),
        re.modificationContext(),
        expectedStickerSelection,
        manualStickerSelection
    );
  }
}

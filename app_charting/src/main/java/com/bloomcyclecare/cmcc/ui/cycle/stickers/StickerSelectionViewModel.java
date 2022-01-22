package com.bloomcyclecare.cmcc.ui.cycle.stickers;

import android.util.Range;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.charting.Cycle;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelection;
import com.bloomcyclecare.cmcc.data.models.training.Exercise;
import com.bloomcyclecare.cmcc.data.repos.sticker.RWStickerSelectionRepo;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.ui.cycle.RenderedEntry;

import org.joda.time.LocalDate;

import java.util.Map;
import java.util.Optional;

import io.reactivex.Completable;
import io.reactivex.Flowable;

public class StickerSelectionViewModel {

  private final RWStickerSelectionRepo mRepo;
  private final ViewMode mViewMode;

  private StickerSelectionViewModel(RWStickerSelectionRepo repo, ViewMode viewMode) {
    mRepo = repo;
    mViewMode = viewMode;
  }

  public static StickerSelectionViewModel forExercise(
      Exercise exercise, ChartingApp app) {
    return new StickerSelectionViewModel(app.stickerSelectionRepo(exercise), ViewMode.CHARTING);
  }

  public static StickerSelectionViewModel forViewMode(
      ViewMode viewMode, ChartingApp app) {
    return new StickerSelectionViewModel(app.stickerSelectionRepo(viewMode), viewMode);
  }

  public Completable updateSticker(LocalDate entryDate, StickerSelection selection) {
    return mRepo.recordSelection(selection, entryDate);
  }

  public Flowable<Map<LocalDate, StickerSelection>> getSelections(Cycle cycle) {
    return mRepo.getSelections(Range.create(cycle.startDate, Optional.ofNullable(cycle.endDate).orElse(LocalDate.now())));
  }

  public ViewMode viewMode() {
    return mViewMode;
  }
}

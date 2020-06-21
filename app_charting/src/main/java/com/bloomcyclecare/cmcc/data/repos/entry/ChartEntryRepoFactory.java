package com.bloomcyclecare.cmcc.data.repos.entry;

import com.bloomcyclecare.cmcc.apps.charting.ViewMode;
import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.models.charting.DemoCycles;
import com.bloomcyclecare.cmcc.data.models.training.Exercise;
import com.bloomcyclecare.cmcc.data.repos.RepoFactory;
import com.bloomcyclecare.cmcc.data.repos.sticker.StickerSelectionRepoFactory;
import com.bloomcyclecare.cmcc.logic.chart.ObservationParser;

import java.util.Optional;

import androidx.annotation.NonNull;
import timber.log.Timber;

public class ChartEntryRepoFactory extends RepoFactory<RWChartEntryRepo> {

  private final StickerSelectionRepoFactory mStickerSelectionRepoFactory;
  private final RWChartEntryRepo mChartingRepo;

  public ChartEntryRepoFactory(@NonNull AppDatabase db,
                               @NonNull StickerSelectionRepoFactory stickerSelectionRepoFactory,
                               @NonNull ViewMode fallbackViewMode) {
    super(fallbackViewMode);
    mStickerSelectionRepoFactory = stickerSelectionRepoFactory;
    mChartingRepo = new RoomChartEntryRepo(db, stickerSelectionRepoFactory.forViewMode(ViewMode.CHARTING));
  }

  @Override
  protected Optional<RWChartEntryRepo> forViewModeInternal(ViewMode viewMode) {
    try {
      switch (viewMode) {
        case CHARTING:
          return Optional.of(mChartingRepo);
        case DEMO:
          return Optional.of(new TrainingChartEntryRepo(
              DemoCycles.forRepos(), false,
              mStickerSelectionRepoFactory.forViewMode(ViewMode.DEMO)));
        case TRAINING:
          Timber.w("Use exercise getter!");
          // TODO: drop
          Exercise exercise = Exercise.forID(Exercise.ID.CYCLE_REVIEW_REGULAR_CYCLES).get();
          return Optional.of(new TrainingChartEntryRepo(
              exercise.trainingCycles(),false,
              mStickerSelectionRepoFactory.forExercise(exercise)));
      }
      return Optional.empty();
    } catch (ObservationParser.InvalidObservationException ioe) {
      Timber.e(ioe);
      return Optional.empty();
    }
  }

  @Override
  public RWChartEntryRepo forExercise(Exercise exercise) {
    try {
      return new TrainingChartEntryRepo(
          exercise.trainingCycles(), false,
          mStickerSelectionRepoFactory.forExercise(exercise));
    } catch (ObservationParser.InvalidObservationException ioe) {
      throw new IllegalStateException(ioe);
    }
  }
}

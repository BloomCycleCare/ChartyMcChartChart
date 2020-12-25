package com.bloomcyclecare.cmcc.data.repos.entry;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.models.charting.DemoCycles;
import com.bloomcyclecare.cmcc.data.models.observation.Observation;
import com.bloomcyclecare.cmcc.data.models.training.Exercise;
import com.bloomcyclecare.cmcc.data.repos.RepoFactory;
import com.bloomcyclecare.cmcc.data.repos.sticker.StickerSelectionRepoFactory;

import java.util.Optional;
import java.util.function.Function;

import androidx.annotation.NonNull;
import timber.log.Timber;

public class ChartEntryRepoFactory extends RepoFactory<RWChartEntryRepo> {

  private final Function<String, Optional<Observation>> mObservationParser;
  private final StickerSelectionRepoFactory mStickerSelectionRepoFactory;
  private final RWChartEntryRepo mChartingRepo;

  public ChartEntryRepoFactory(@NonNull AppDatabase db,
                               @NonNull StickerSelectionRepoFactory stickerSelectionRepoFactory,
                               @NonNull ViewMode fallbackViewMode,
                               @NonNull Function<String, Optional<Observation>> observationParser) {
    super(fallbackViewMode);
    mStickerSelectionRepoFactory = stickerSelectionRepoFactory;
    mChartingRepo = new RoomChartEntryRepo(db, stickerSelectionRepoFactory.forViewMode(ViewMode.CHARTING));
    mObservationParser = observationParser;
  }

  @Override
  protected Optional<RWChartEntryRepo> forViewModeInternal(ViewMode viewMode) {
    switch (viewMode) {
      case CHARTING:
        return Optional.of(mChartingRepo);
      case DEMO:
        return Optional.of(new TrainingChartEntryRepo(
            DemoCycles.forRepos(), false,
            mStickerSelectionRepoFactory.forViewMode(ViewMode.DEMO), mObservationParser));
      case TRAINING:
        Timber.w("Use exercise getter!");
        // TODO: drop
        Optional<Exercise> exercise = Exercise.forID(Exercise.ID.CYCLE_REVIEW_REGULAR_CYCLES);
        if (!exercise.isPresent()) {
          throw new IllegalStateException("Could not find exercise for ID " + Exercise.ID.CYCLE_REVIEW_REGULAR_CYCLES);
        }
        return Optional.of(new TrainingChartEntryRepo(
            exercise.get().trainingCycles(),false,
            mStickerSelectionRepoFactory.forExercise(exercise.get()), mObservationParser));
    }
    return Optional.empty();
  }

  @Override
  public RWChartEntryRepo forExercise(Exercise exercise) {
    return new TrainingChartEntryRepo(
        exercise.trainingCycles(), false,
        mStickerSelectionRepoFactory.forExercise(exercise), mObservationParser);
  }
}

package com.bloomcyclecare.cmcc.data.repos.entry;

import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.models.DemoCycles;
import com.bloomcyclecare.cmcc.data.models.Exercise;
import com.bloomcyclecare.cmcc.data.models.TrainingCycles;
import com.bloomcyclecare.cmcc.data.repos.RepoFactory;
import com.bloomcyclecare.cmcc.logic.chart.ObservationParser;

import java.util.Optional;

import androidx.annotation.NonNull;
import timber.log.Timber;

public class ChartEntryRepoFactory extends RepoFactory<RWChartEntryRepo> {

  private final RWChartEntryRepo mChartingRepo;

  public ChartEntryRepoFactory(@NonNull AppDatabase db,
                               @NonNull ViewMode fallbackViewMode) {
    super(fallbackViewMode);
    mChartingRepo = new RoomChartEntryRepo(db);
  }

  @Override
  protected Optional<RWChartEntryRepo> forViewModeInternal(ViewMode viewMode) {
    try {
      switch (viewMode) {
        case CHARTING:
          return Optional.of(mChartingRepo);
        case DEMO:
          return Optional.of(new TrainingChartEntryRepo(DemoCycles.forRepos(), false));
        case TRAINING:
          return Optional.of(new TrainingChartEntryRepo(TrainingCycles.REGULAR_CYCLES ,false));
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
      return new TrainingChartEntryRepo(exercise.trainingCycles(), false);
    } catch (ObservationParser.InvalidObservationException ioe) {
      throw new IllegalStateException(ioe);
    }
  }
}

package com.bloomcyclecare.cmcc.data.repos.cycle;

import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.models.DemoCycles;
import com.bloomcyclecare.cmcc.data.models.Exercise;
import com.bloomcyclecare.cmcc.data.models.TrainingCycles;
import com.bloomcyclecare.cmcc.data.repos.RepoFactory;

import org.joda.time.LocalDate;

import java.util.Optional;

import androidx.annotation.NonNull;

public class CycleRepoFactory extends RepoFactory<RWCycleRepo> {

  private final RWCycleRepo mChartingRepo;

  public CycleRepoFactory(@NonNull AppDatabase db, ViewMode fallbackViewMode) {
    super(fallbackViewMode);
    mChartingRepo = new RoomCycleRepo(db);
  }

  @Override
  protected Optional<RWCycleRepo> forViewModeInternal(ViewMode viewMode) {
    switch (viewMode) {
      case CHARTING:
        return Optional.of(mChartingRepo);
      case DEMO:
        return Optional.of(new TrainingCycleRepo(DemoCycles.forRepos(), LocalDate::now));
      case TRAINING:
        return Optional.of(new TrainingCycleRepo(TrainingCycles.REGULAR_CYCLES, LocalDate::now));
    }
    return Optional.empty();
  }

  @Override
  public RWCycleRepo forExercise(Exercise exercise) {
    return new TrainingCycleRepo(exercise.trainingCycles(), LocalDate::now);
  }
}

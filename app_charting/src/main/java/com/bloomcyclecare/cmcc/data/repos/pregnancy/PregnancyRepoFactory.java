package com.bloomcyclecare.cmcc.data.repos.pregnancy;

import com.bloomcyclecare.cmcc.apps.charting.ViewMode;
import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.models.Exercise;
import com.bloomcyclecare.cmcc.data.repos.RepoFactory;
import com.bloomcyclecare.cmcc.data.repos.cycle.CycleRepoFactory;

import java.util.Optional;

public class PregnancyRepoFactory extends RepoFactory<RWPregnancyRepo> {

  private final RWPregnancyRepo mChartingRepo;

  public PregnancyRepoFactory(AppDatabase db, CycleRepoFactory cycleRepoFactory, ViewMode fallbackViewMode) {
    super(fallbackViewMode);
    this.mChartingRepo = new RoomPregnancyRepo(db, cycleRepoFactory.forViewMode(ViewMode.CHARTING));
  }

  @Override
  protected Optional<RWPregnancyRepo> forViewModeInternal(ViewMode viewMode) {
    switch (viewMode) {
      case CHARTING:
        return Optional.of(mChartingRepo);
      case TRAINING:
      case DEMO:
        return Optional.of(new TrainingPregnancyRepo());
    }
    return Optional.empty();
  }

  @Override
  public RWPregnancyRepo forExercise(Exercise exercise) {
    return forViewMode(ViewMode.TRAINING);
  }
}

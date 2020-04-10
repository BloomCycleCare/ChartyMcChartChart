package com.bloomcyclecare.cmcc.data.repos.pregnancy;

import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.repos.RepoFactory;
import com.bloomcyclecare.cmcc.data.repos.cycle.CycleRepoFactory;

import java.util.Optional;

public class PregnancyRepoFactory extends RepoFactory<RWPregnancyRepo> {

  private final RWPregnancyRepo mChartingRepo;

  public PregnancyRepoFactory(AppDatabase db, CycleRepoFactory cycleRepoFactory) {
    this.mChartingRepo = new RoomPregnancyRepo(
        db, cycleRepoFactory.forViewMode(ViewMode.CHARTING, ViewMode.CHARTING));
  }

  @Override
  protected Optional<RWPregnancyRepo> forViewMode(ViewMode viewMode) {
    switch (viewMode) {
      case CHARTING:
        return Optional.of(mChartingRepo);
      case TRAINING:
      case DEMO:
        return Optional.of(new TrainingPregnancyRepo());
    }
    return Optional.empty();
  }
}

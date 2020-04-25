package com.bloomcyclecare.cmcc.data.repos.instructions;

import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.models.DemoCycles;
import com.bloomcyclecare.cmcc.data.models.TrainingCycles;
import com.bloomcyclecare.cmcc.data.repos.RepoFactory;

import org.joda.time.LocalDate;

import java.util.Optional;

public class InstructionsRepoFactory extends RepoFactory<RWInstructionsRepo> {

  private final RWInstructionsRepo mChartingRepo;

  public InstructionsRepoFactory(AppDatabase db, ViewMode fallbackViewMode) {
    super(fallbackViewMode);
    mChartingRepo = new RoomInstructionsRepo(db);
  }

  @Override
  protected Optional<RWInstructionsRepo> forViewModeInternal(ViewMode viewMode) {
    switch (viewMode) {
      case CHARTING:
        return Optional.of(mChartingRepo);
      case TRAINING:
        return Optional.of(new TrainingInstructionsRepo(TrainingCycles.REGULAR_CYCLES, LocalDate::now));
      case DEMO:
        return Optional.of(new TrainingInstructionsRepo(DemoCycles.forRepos(), LocalDate::now));
    }
    return Optional.empty();
  }
}

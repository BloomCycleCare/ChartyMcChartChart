package com.bloomcyclecare.cmcc.data.repos.medication;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.models.training.Exercise;
import com.bloomcyclecare.cmcc.data.repos.RepoFactory;

import java.util.Optional;

public class MedicationRepoFactory extends RepoFactory<RWMedicationRepo> {

  private final RoomMedicationRepo mRoomMedicationRepo;

  public MedicationRepoFactory(AppDatabase db, ViewMode fallbackViewMode) {
    super(fallbackViewMode);
    mRoomMedicationRepo = new RoomMedicationRepo(db);
  }

  @Override
  protected Optional<RWMedicationRepo> forViewModeInternal(ViewMode viewMode) {
    return Optional.of(mRoomMedicationRepo);
  }

  @Override
  public RWMedicationRepo forExercise(Exercise exercise) {
    return forViewMode(ViewMode.TRAINING);
  }
}

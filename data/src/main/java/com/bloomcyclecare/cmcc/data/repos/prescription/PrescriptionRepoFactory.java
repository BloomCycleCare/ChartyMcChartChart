package com.bloomcyclecare.cmcc.data.repos.prescription;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.models.training.Exercise;
import com.bloomcyclecare.cmcc.data.repos.RepoFactory;

import java.util.Optional;

public class PrescriptionRepoFactory extends RepoFactory<RWPrescriptionRepo> {

  private final RoomPrescriptionRepo mRoomPrescriptionRepo;

  public PrescriptionRepoFactory(AppDatabase db, ViewMode fallbackViewMode) {
    super(fallbackViewMode);
    mRoomPrescriptionRepo = new RoomPrescriptionRepo(db.prescriptionDao());
  }

  @Override
  protected Optional<RWPrescriptionRepo> forViewModeInternal(ViewMode viewMode) {
    return Optional.of(mRoomPrescriptionRepo);
  }

  @Override
  public RWPrescriptionRepo forExercise(Exercise exercise) {
    return forViewMode(ViewMode.TRAINING);
  }
}

package com.bloomcyclecare.cmcc.data.repos.sticker;

import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.models.Exercise;
import com.bloomcyclecare.cmcc.data.repos.RepoFactory;

import java.util.Optional;

import androidx.annotation.NonNull;

public class StickerSelectionRepoFactory extends RepoFactory<RWStickerSelectionRepo> {

  private final RoomStickerSelectionRepo mRoomRepo;
  private final TrainingStickerSelectionRepo mInmemoryRepo;

  public StickerSelectionRepoFactory(@NonNull AppDatabase db, @NonNull ViewMode fallbackViewMode) {
    super(fallbackViewMode);
    mInmemoryRepo = new TrainingStickerSelectionRepo();
    mRoomRepo = new RoomStickerSelectionRepo(db);
  }

  @Override
  protected Optional<RWStickerSelectionRepo> forViewModeInternal(ViewMode viewMode) {
    switch (viewMode) {
      case CHARTING:
        return Optional.of(mRoomRepo);
      case TRAINING:
      case DEMO:
        return Optional.of(mInmemoryRepo);
      default:
        return Optional.empty();
    }
  }

  @Override
  public RWStickerSelectionRepo forExercise(Exercise exercise) {
    return forViewMode(ViewMode.TRAINING);
  }
}

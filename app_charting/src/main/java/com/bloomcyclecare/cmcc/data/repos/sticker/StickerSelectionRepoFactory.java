package com.bloomcyclecare.cmcc.data.repos.sticker;

import com.bloomcyclecare.cmcc.apps.charting.ViewMode;
import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.models.training.Exercise;
import com.bloomcyclecare.cmcc.data.repos.RepoFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Optional;

import androidx.annotation.NonNull;
import timber.log.Timber;

public class StickerSelectionRepoFactory extends RepoFactory<RWStickerSelectionRepo> {

  private final RoomStickerSelectionRepo mRoomRepo;
  private final TrainingStickerSelectionRepo mInmemoryRepo;
  private final Cache<Exercise, RWStickerSelectionRepo> mTrainingRepoCache = CacheBuilder.newBuilder()
      .maximumSize(Exercise.COLLECTION.size())
      .build();

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
    try {
      return mTrainingRepoCache.get(exercise, TrainingStickerSelectionRepo::new);
    } catch (Exception e) {
      Timber.wtf(e);
      return new TrainingStickerSelectionRepo();
    }
  }
}

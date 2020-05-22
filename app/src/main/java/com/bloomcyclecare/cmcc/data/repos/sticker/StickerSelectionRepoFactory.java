package com.bloomcyclecare.cmcc.data.repos.sticker;

import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.models.Exercise;
import com.bloomcyclecare.cmcc.data.repos.RepoFactory;

import java.util.Optional;

public class StickerSelectionRepoFactory extends RepoFactory<RWStickerSelectionRepo> {

  private final TrainingStickerSelectionRepo mRepo;

  public StickerSelectionRepoFactory(ViewMode fallbackViewMode) {
    super(fallbackViewMode);
    mRepo = new TrainingStickerSelectionRepo();
  }

  @Override
  protected Optional<RWStickerSelectionRepo> forViewModeInternal(ViewMode viewMode) {
    return Optional.of(mRepo);
  }

  @Override
  public RWStickerSelectionRepo forExercise(Exercise exercise) {
    return forViewMode(ViewMode.TRAINING);
  }
}

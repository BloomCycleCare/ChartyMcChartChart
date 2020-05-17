package com.bloomcyclecare.cmcc.data.repos.sticker;

import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.models.Exercise;
import com.bloomcyclecare.cmcc.data.repos.RepoFactory;

import java.util.Optional;

public class StickerSelectionRepoFactory extends RepoFactory<RWStickerSelectionRepo> {

  private final TrainingStickerSelectionRepo repo = new TrainingStickerSelectionRepo();

  public StickerSelectionRepoFactory(ViewMode fallbackViewMode) {
    super(fallbackViewMode);
  }

  @Override
  protected Optional<RWStickerSelectionRepo> forViewModeInternal(ViewMode viewMode) {
    return Optional.of(repo);
  }

  @Override
  public RWStickerSelectionRepo forExercise(Exercise exercise) {
    return forViewMode(ViewMode.TRAINING);
  }
}

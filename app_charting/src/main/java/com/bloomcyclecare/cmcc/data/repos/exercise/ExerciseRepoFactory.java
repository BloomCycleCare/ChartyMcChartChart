package com.bloomcyclecare.cmcc.data.repos.exercise;

import com.bloomcyclecare.cmcc.apps.charting.ViewMode;
import com.bloomcyclecare.cmcc.data.models.Exercise;
import com.bloomcyclecare.cmcc.data.repos.RepoFactory;

import java.util.Optional;

public class ExerciseRepoFactory extends RepoFactory<RWExerciseRepo> {

  public ExerciseRepoFactory(ViewMode fallbackViewMode) {
    super(fallbackViewMode);
  }

  @Override
  protected Optional<RWExerciseRepo> forViewModeInternal(ViewMode viewMode) {
    if (viewMode == ViewMode.TRAINING) {
      return Optional.of(new TrainingExerciseRepo());
    }
    return Optional.empty();
  }

  @Override
  public RWExerciseRepo forExercise(Exercise exercise) {
    return forViewMode(ViewMode.TRAINING);
  }
}

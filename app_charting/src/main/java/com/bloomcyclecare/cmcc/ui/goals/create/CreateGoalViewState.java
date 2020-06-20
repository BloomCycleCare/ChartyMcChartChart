package com.bloomcyclecare.cmcc.ui.goals.create;

import com.bloomcyclecare.cmcc.logic.goals.GoalModel;
import com.bloomcyclecare.cmcc.mvi.MviViewState;
import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@AutoValue
abstract class CreateGoalViewState implements MviViewState {

  abstract List<GoalModel> goalModels();

  abstract boolean isSaved();

  @Nullable
  abstract Throwable error();

  public abstract Builder buildWith();

  public static CreateGoalViewState idle() {
    return new AutoValue_CreateGoalViewState.Builder()
        .goalModels(Collections.emptyList())
        .isSaved(false)
        .build();
  }

  @AutoValue.Builder
  static abstract class Builder {
    abstract Builder goalModels(List<GoalModel> goalModels);

    abstract Builder isSaved(boolean isSaved);

    abstract Builder error(@NonNull Throwable error);

    abstract CreateGoalViewState build();
  }
}

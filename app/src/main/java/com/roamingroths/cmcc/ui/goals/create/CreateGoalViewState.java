package com.roamingroths.cmcc.ui.goals.create;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.roamingroths.cmcc.logic.goals.GoalModel;
import com.roamingroths.cmcc.mvi.MviViewState;

import java.util.Collections;
import java.util.List;

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

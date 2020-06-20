package com.bloomcyclecare.cmcc.ui.goals.list;

import com.bloomcyclecare.cmcc.logic.goals.Goal;
import com.bloomcyclecare.cmcc.logic.goals.GoalFilterType;
import com.bloomcyclecare.cmcc.mvi.MviViewState;
import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * Created by parkeroth on 3/3/18.
 */

@AutoValue
abstract class GoalListViewState implements MviViewState {

  public abstract boolean isLoading();

  public abstract GoalFilterType goalFilterType();

  public abstract List<Goal> goals();

  @Nullable
  abstract Throwable error();

  public abstract Builder buildWith();

  static GoalListViewState idle() {
    return new AutoValue_GoalListViewState.Builder()
        .isLoading(false)
        .goalFilterType(GoalFilterType.ALL)
        .goals(Collections.emptyList())
        .error(null)
        .build();
  }

  @AutoValue.Builder
  static abstract class Builder {
    abstract Builder isLoading(boolean isLoading);

    abstract Builder goalFilterType(GoalFilterType goalFilterType);

    abstract Builder goals(@Nullable List<Goal> goals);

    abstract Builder error(@Nullable Throwable throwable);

    abstract GoalListViewState build();
  }
}

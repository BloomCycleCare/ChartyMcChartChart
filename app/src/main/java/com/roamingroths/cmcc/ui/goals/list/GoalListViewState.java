package com.roamingroths.cmcc.ui.goals.list;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.roamingroths.cmcc.logic.goals.Goal;
import com.roamingroths.cmcc.logic.goals.GoalFilterType;
import com.roamingroths.cmcc.mvi.MviViewState;

import java.util.Collections;
import java.util.List;

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

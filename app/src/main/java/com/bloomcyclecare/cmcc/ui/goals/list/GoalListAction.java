package com.bloomcyclecare.cmcc.ui.goals.list;

import com.bloomcyclecare.cmcc.logic.goals.GoalFilterType;
import com.bloomcyclecare.cmcc.mvi.MviAction;
import com.google.auto.value.AutoValue;

import androidx.annotation.Nullable;

/**
 * Created by parkeroth on 3/3/18.
 */

interface GoalListAction extends MviAction {

  @AutoValue
  abstract class LoadGoals implements GoalListAction {

    @Nullable
    public abstract GoalFilterType filterType();

    public static LoadGoals loadAndFilter(GoalFilterType filterType) {
      return new AutoValue_GoalListAction_LoadGoals(filterType);
    }

    public static LoadGoals load() {
      return new AutoValue_GoalListAction_LoadGoals(null);
    }
  }
}

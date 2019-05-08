package com.roamingroths.cmcc.ui.goals.list;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.roamingroths.cmcc.logic.goals.GoalFilterType;
import com.roamingroths.cmcc.mvi.MviAction;

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

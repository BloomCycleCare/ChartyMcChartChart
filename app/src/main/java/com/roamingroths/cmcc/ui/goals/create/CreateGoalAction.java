package com.roamingroths.cmcc.ui.goals.create;

import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;
import com.roamingroths.cmcc.logic.goals.GoalModel;
import com.roamingroths.cmcc.mvi.MviAction;

interface CreateGoalAction extends MviAction {

  @AutoValue
  abstract class CreateGoal implements CreateGoalAction {

    abstract String input();

    public static CreateGoal create(@NonNull String input) {
      return new AutoValue_CreateGoalAction_CreateGoal(input);
    }
  }

  @AutoValue
  abstract class GetSuggestions implements  CreateGoalAction {

    @NonNull
    abstract String input();

    public static GetSuggestions create(@NonNull String input) {
      return new AutoValue_CreateGoalAction_GetSuggestions(input);
    }
  }

  @AutoValue
  abstract class SkipMe implements CreateGoalAction {

    public static SkipMe create() {
      return new AutoValue_CreateGoalAction_SkipMe();
    }
  }
}

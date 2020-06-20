package com.bloomcyclecare.cmcc.ui.goals.create;

import com.bloomcyclecare.cmcc.mvi.MviAction;
import com.google.auto.value.AutoValue;

import androidx.annotation.NonNull;

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

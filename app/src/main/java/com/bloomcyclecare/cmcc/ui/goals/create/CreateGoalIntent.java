package com.bloomcyclecare.cmcc.ui.goals.create;

import com.bloomcyclecare.cmcc.mvi.MviIntent;
import com.google.auto.value.AutoValue;

/**
 * Created by parkeroth on 3/4/18.
 */

interface CreateGoalIntent extends MviIntent {

  @AutoValue
  abstract class InitialIntent implements CreateGoalIntent {

    public static InitialIntent create() {
      return new AutoValue_CreateGoalIntent_InitialIntent();
    }
  }

  @AutoValue
  abstract class GetSuggestions implements CreateGoalIntent {

    abstract String input();

    public static GetSuggestions create(String input) {
      return new AutoValue_CreateGoalIntent_GetSuggestions(input);
    }
  }

  @AutoValue
  abstract class SaveGoal implements CreateGoalIntent {

    abstract String input();

    public static SaveGoal create(String input) {
      return new AutoValue_CreateGoalIntent_SaveGoal(input);
    }
  }
}

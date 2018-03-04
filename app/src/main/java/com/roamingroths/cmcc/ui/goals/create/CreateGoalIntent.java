package com.roamingroths.cmcc.ui.goals.create;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.roamingroths.cmcc.logic.goals.GoalModel;
import com.roamingroths.cmcc.mvi.MviIntent;

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

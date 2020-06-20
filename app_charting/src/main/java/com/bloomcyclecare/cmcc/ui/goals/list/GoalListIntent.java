package com.bloomcyclecare.cmcc.ui.goals.list;

import com.bloomcyclecare.cmcc.mvi.MviIntent;
import com.google.auto.value.AutoValue;

/**
 * Created by parkeroth on 3/3/18.
 */

interface GoalListIntent extends MviIntent {

  @AutoValue
  abstract class InitialIntent implements GoalListIntent {
    public static InitialIntent create() {
      return new AutoValue_GoalListIntent_InitialIntent();
    }
  }

  @AutoValue
  abstract class RefreshIntent implements GoalListIntent {
    public static RefreshIntent create() {
      return new AutoValue_GoalListIntent_RefreshIntent();
    }
  }
}

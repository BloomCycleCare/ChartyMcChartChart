package com.roamingroths.cmcc.ui.goals.list;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.roamingroths.cmcc.logic.goals.Goal;
import com.roamingroths.cmcc.logic.goals.GoalFilterType;
import com.roamingroths.cmcc.mvi.MviResult;
import com.roamingroths.cmcc.utils.TaskStatus;

import java.util.List;

/**
 * Created by parkeroth on 3/3/18.
 */

public interface GoalListResult extends MviResult {

  @AutoValue
  abstract class LoadGoals implements GoalListResult {
    @NonNull
    abstract TaskStatus status();

    @Nullable
    abstract List<Goal> goals();

    @Nullable
    abstract GoalFilterType filterType();

    @Nullable
    abstract Throwable error();

    @NonNull
    static LoadGoals success(@NonNull List<Goal> goals, @Nullable GoalFilterType filterType) {
      return new AutoValue_GoalListResult_LoadGoals(TaskStatus.SUCCESS, goals, filterType, null);
    }

    @NonNull
    static LoadGoals failure(Throwable error) {
      return new AutoValue_GoalListResult_LoadGoals(TaskStatus.FAILURE, null, null, error);
    }

    @NonNull
    static LoadGoals inFlight() {
      return new AutoValue_GoalListResult_LoadGoals(TaskStatus.IN_FLIGHT, null, null, null);
    }
  }
}

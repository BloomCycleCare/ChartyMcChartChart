package com.bloomcyclecare.cmcc.ui.goals.list;

import com.bloomcyclecare.cmcc.logic.goals.Goal;
import com.bloomcyclecare.cmcc.logic.goals.GoalFilterType;
import com.bloomcyclecare.cmcc.mvi.MviResult;
import com.bloomcyclecare.cmcc.utils.TaskStatus;
import com.google.auto.value.AutoValue;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

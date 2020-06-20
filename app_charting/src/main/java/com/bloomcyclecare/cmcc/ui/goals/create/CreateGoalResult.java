package com.bloomcyclecare.cmcc.ui.goals.create;

import com.bloomcyclecare.cmcc.logic.goals.GoalModel;
import com.bloomcyclecare.cmcc.mvi.MviResult;
import com.bloomcyclecare.cmcc.utils.TaskStatus;
import com.google.auto.value.AutoValue;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by parkeroth on 3/4/18.
 */

public interface CreateGoalResult extends MviResult {

  @AutoValue
  abstract class GetSuggestions implements CreateGoalResult {

    @NonNull
    abstract TaskStatus status();

    @Nullable
    abstract List<GoalModel> models();

    @Nullable
    abstract Throwable error();

    @NonNull
    static GetSuggestions success(@NonNull List<GoalModel> models) {
      return new AutoValue_CreateGoalResult_GetSuggestions(TaskStatus.SUCCESS, models, null);
    }

    @NonNull
    static GetSuggestions failure(Throwable error) {
      return new AutoValue_CreateGoalResult_GetSuggestions(TaskStatus.FAILURE, null, error);
    }

    @NonNull
    static GetSuggestions inFlight() {
      return new AutoValue_CreateGoalResult_GetSuggestions(TaskStatus.IN_FLIGHT, null, null);
    }
  }

  @AutoValue
  abstract class CreateGoal implements CreateGoalResult {

    @NonNull
    abstract TaskStatus status();

    @Nullable
    abstract Throwable error();

    @NonNull
    static CreateGoal success() {
      return new AutoValue_CreateGoalResult_CreateGoal(TaskStatus.SUCCESS, null);
    }

    @NonNull
    static CreateGoal failure(Throwable error) {
      return new AutoValue_CreateGoalResult_CreateGoal(TaskStatus.FAILURE, error);
    }

    @NonNull
    static CreateGoal inFlight() {
      return new AutoValue_CreateGoalResult_CreateGoal(TaskStatus.IN_FLIGHT, null);
    }
  }
}

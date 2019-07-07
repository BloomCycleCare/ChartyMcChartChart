package com.roamingroths.cmcc.ui.goals.create;

import com.roamingroths.cmcc.mvi.BaseMviViewModel;
import com.roamingroths.cmcc.mvi.MviIntent;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by parkeroth on 3/4/18.
 */

public class CreateGoalViewModel extends BaseMviViewModel<CreateGoalIntent, CreateGoalAction, CreateGoalResult, CreateGoalViewState> {

  public static CreateGoalViewModel create() {
    return new CreateGoalViewModel(new CreateGoalProcessors());
  }

  public CreateGoalViewModel(CreateGoalProcessors processors) {
    super(CreateGoalIntent.InitialIntent.class, CreateGoalViewState.idle(), processors);
  }

  @Override
  protected CreateGoalAction actionFromIntent(MviIntent intent) {
    if (intent instanceof CreateGoalIntent.InitialIntent) {
      return CreateGoalAction.GetSuggestions.create("");
    }
    if (intent instanceof CreateGoalIntent.GetSuggestions) {
      CreateGoalIntent.GetSuggestions getSuggestionsIntent = (CreateGoalIntent.GetSuggestions) intent;
      return CreateGoalAction.GetSuggestions.create(getSuggestionsIntent.input());
    }
    if (intent instanceof CreateGoalIntent.SaveGoal) {
      CreateGoalIntent.SaveGoal saveGoalIntent = (CreateGoalIntent.SaveGoal) intent;
      return CreateGoalAction.CreateGoal.create(saveGoalIntent.input());
    }
    throw new IllegalArgumentException("Unknown Intent: " + intent);
  }

  @Override
  protected CreateGoalViewState reducer(CreateGoalViewState previousState, CreateGoalResult result) {
    CreateGoalViewState.Builder stateBuilder = previousState.buildWith();

    if (result instanceof CreateGoalResult.GetSuggestions) {
      CreateGoalResult.GetSuggestions getSuggestionsResult = (CreateGoalResult.GetSuggestions) result;
      switch (getSuggestionsResult.status()) {
        case SUCCESS:
          stateBuilder.goalModels(getSuggestionsResult.models());
          return stateBuilder.build();
        case FAILURE:
          Throwable error = checkNotNull(getSuggestionsResult.error());
          return stateBuilder.error(error).build();
        case IN_FLIGHT:
          return stateBuilder.build();
      }
    }

    if (result instanceof CreateGoalResult.CreateGoal) {
      CreateGoalResult.CreateGoal createGoalResult = (CreateGoalResult.CreateGoal) result;
      switch (createGoalResult.status()) {
        case SUCCESS:
          return stateBuilder.isSaved(true).build();
        case FAILURE:
          Throwable error = checkNotNull(createGoalResult.error());
          return stateBuilder.error(error).build();
        case IN_FLIGHT:
          return stateBuilder.build();
      }
    }

    throw new IllegalArgumentException("Unknown result" + result);
  }
}

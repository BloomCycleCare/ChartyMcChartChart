package com.roamingroths.cmcc.ui.goals.list;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.roamingroths.cmcc.logic.goals.Goal;
import com.roamingroths.cmcc.logic.goals.GoalFilterType;
import com.roamingroths.cmcc.mvi.BaseMviViewModel;
import com.roamingroths.cmcc.mvi.MviIntent;
import com.roamingroths.cmcc.providers.GoalProvider;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by parkeroth on 3/3/18.
 */

public class GoalListViewModel extends BaseMviViewModel<GoalListIntent, GoalListAction, GoalListResult, GoalListViewState> {

  public static GoalListViewModel create(GoalProvider goalProvider) {
    GoalListProcessors processors = new GoalListProcessors(goalProvider);
    return new GoalListViewModel(processors);
  }

  public GoalListViewModel(GoalListProcessors processors) {
    super(GoalListIntent.InitialIntent.class, GoalListViewState.idle(), processors);
  }

  @Override
  protected GoalListAction actionFromIntent(MviIntent intent) {
    if (intent instanceof GoalListIntent.InitialIntent) {
      return GoalListAction.LoadGoals.loadAndFilter(GoalFilterType.ALL);
    }
    if (intent instanceof GoalListIntent.RefreshIntent) {
      return GoalListAction.LoadGoals.loadAndFilter(GoalFilterType.ALL);
    }
    throw new IllegalArgumentException("Unknown intent " + intent);
  }

  @Override
  protected GoalListViewState reducer(GoalListViewState previousState, GoalListResult result) {
    GoalListViewState.Builder stateBuilder = previousState.buildWith();
    if (result instanceof GoalListResult.LoadGoals) {
      GoalListResult.LoadGoals loadResult = (GoalListResult.LoadGoals) result;
      switch (loadResult.status()) {
        case SUCCESS:
          GoalFilterType filterType = loadResult.filterType();
          if (filterType == null) {
            filterType = previousState.goalFilterType();
          }
          List<Goal> goals = filteredGoals(checkNotNull(loadResult.goals()), filterType);
          return stateBuilder.isLoading(false).goals(goals).goalFilterType(filterType).build();
        case FAILURE:
          return stateBuilder.isLoading(false).error(loadResult.error()).build();
        case IN_FLIGHT:
          return stateBuilder.isLoading(true).build();
      }
    } else {
      throw new IllegalArgumentException("Unknown result: " + result);
    }
    throw new IllegalStateException();
  }

  private static List<Goal> filteredGoals(List<Goal> goals, GoalFilterType filterType) {
    switch (filterType) {
      case ALL:
        return goals;
      case ACTIVE:
        return Lists.newArrayList(Iterables.filter(goals, goal -> goal.status == Goal.Status.ACTIVE));
      case ARCHIVED:
        return Lists.newArrayList(Iterables.filter(goals, goal -> goal.status == Goal.Status.ARCHIVED));
    }
    throw new IllegalArgumentException();
  }
}

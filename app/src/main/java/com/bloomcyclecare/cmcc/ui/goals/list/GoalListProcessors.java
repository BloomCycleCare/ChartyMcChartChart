package com.bloomcyclecare.cmcc.ui.goals.list;

import com.bloomcyclecare.cmcc.mvi.MviProcessors;

import io.reactivex.ObservableTransformer;

/**
 * Created by parkeroth on 3/4/18.
 */
class GoalListProcessors extends MviProcessors<GoalListAction, GoalListResult> {

  GoalListProcessors() {
    super();

    registerTransformer(GoalListAction.LoadGoals.class, loadGoalsProcessor());
  }

  private final ObservableTransformer<GoalListAction.LoadGoals, GoalListResult.LoadGoals> loadGoalsProcessor() {
    /*return actions -> actions.flatMap(action -> mGoalProvider
        .getGoals()
        .toList()
        .toObservable()
        .map(goals -> GoalListResult.LoadGoals.success(goals, action.filterType()))
        .onErrorReturn(GoalListResult.LoadGoals::failure)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .startWith(GoalListResult.LoadGoals.inFlight()));*/
    return actions -> actions.map(action -> GoalListResult.LoadGoals.inFlight());
  }
}

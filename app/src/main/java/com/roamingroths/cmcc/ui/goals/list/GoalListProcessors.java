package com.roamingroths.cmcc.ui.goals.list;

import com.roamingroths.cmcc.mvi.MviProcessors;
import com.roamingroths.cmcc.providers.GoalProvider;

import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by parkeroth on 3/4/18.
 */
class GoalListProcessors extends MviProcessors<GoalListAction, GoalListResult> {

  private final GoalProvider mGoalProvider;

  GoalListProcessors(GoalProvider mGoalProvider) {
    super();
    this.mGoalProvider = mGoalProvider;

    registerTransformer(GoalListAction.LoadGoals.class, loadGoalsProcessor());
  }

  private final ObservableTransformer<GoalListAction.LoadGoals, GoalListResult.LoadGoals> loadGoalsProcessor() {
    return actions -> actions.flatMap(action -> mGoalProvider
        .getGoals()
        .toList()
        .toObservable()
        .map(goals -> GoalListResult.LoadGoals.success(goals, action.filterType()))
        .onErrorReturn(GoalListResult.LoadGoals::failure)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .startWith(GoalListResult.LoadGoals.inFlight()));
  }
}

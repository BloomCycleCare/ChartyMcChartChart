package com.roamingroths.cmcc.ui.goals.create;

import com.google.common.collect.Iterables;
import com.roamingroths.cmcc.logic.goals.GoalModel;
import com.roamingroths.cmcc.logic.goals.GoalModelFactory;
import com.roamingroths.cmcc.mvi.MviProcessors;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by parkeroth on 3/4/18.
 */

class CreateGoalProcessors extends MviProcessors<CreateGoalAction, CreateGoalResult> {

  private final GoalModelFactory mModelFactory;

  CreateGoalProcessors() {
    super();
    this.mModelFactory = GoalModelFactory.withDefaultTemplates();

    registerTransformer(CreateGoalAction.GetSuggestions.class, loadGoalsProcessor());
    registerTransformer(CreateGoalAction.CreateGoal.class, saveGoalProcessor());
  }

  private ObservableTransformer<CreateGoalAction.GetSuggestions, CreateGoalResult.GetSuggestions> loadGoalsProcessor() {
    return actions -> actions
        .map(action -> mModelFactory.fromInput(action.input(), 2))
        .map(CreateGoalResult.GetSuggestions::success)
        .onErrorReturn(CreateGoalResult.GetSuggestions::failure)
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
        .startWith(CreateGoalResult.GetSuggestions.inFlight());
  }

  private ObservableTransformer<CreateGoalAction.CreateGoal, CreateGoalResult.CreateGoal> saveGoalProcessor() {
    return actions -> actions
        .map(action -> mModelFactory.fromInput(action.input(), 2))
        .flatMap(suggestions -> {
          GoalModel model = Iterables.getOnlyElement(suggestions);
          return Observable.just(CreateGoalResult.CreateGoal.success());
        })
        .onErrorReturn(CreateGoalResult.CreateGoal::failure)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .startWith(CreateGoalResult.CreateGoal.inFlight());
  }
}

package com.roamingroths.cmcc.application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.roamingroths.cmcc.ui.goals.create.CreateGoalViewModel;
import com.roamingroths.cmcc.ui.goals.list.GoalListViewModel;

/**
 * Created by parkeroth on 3/3/18.
 */

public class ViewModelFactory implements ViewModelProvider.Factory {

  @NonNull
  @Override
  @SuppressWarnings("unchecked")
  public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
    if (modelClass == GoalListViewModel.class) {
      return (T) GoalListViewModel.create();
    }
    if (modelClass == CreateGoalViewModel.class) {
      return (T) CreateGoalViewModel.create();
    }
    throw new IllegalArgumentException("Unknown model class " + modelClass);
  }
}

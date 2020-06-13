package com.bloomcyclecare.cmcc.ui.init.ftue;

import android.content.Context;

import com.bloomcyclecare.cmcc.ui.init.ftue.steps.InitCycleStepFragment;
import com.bloomcyclecare.cmcc.ui.init.ftue.steps.InstructionsStepFragment;
import com.bloomcyclecare.cmcc.ui.init.ftue.steps.StartStepFragment;
import com.bloomcyclecare.cmcc.ui.init.ftue.steps.TosStepFragment;
import com.stepstone.stepper.Step;
import com.stepstone.stepper.adapter.AbstractFragmentStepAdapter;
import com.stepstone.stepper.viewmodel.StepViewModel;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

public class StepAdapter extends AbstractFragmentStepAdapter {

  public StepAdapter(@NonNull FragmentManager fm, @NonNull Context context) {
    super(fm, context);
  }

  @Override
  public Step createStep(int position) {
    switch (position) {
      case 0:
        return new StartStepFragment();
      case 1:
        return new TosStepFragment();
      case 2:
        return new InitCycleStepFragment();
      case 3:
        return new InstructionsStepFragment();
    }
    return null;
  }

  @NonNull
  @Override
  public StepViewModel getViewModel(int position) {
    StepViewModel.Builder builder = new StepViewModel.Builder(context);
    switch (position) {
      case 0:
        builder.setEndButtonLabel("Get Started");
        return builder.create();
      default:
        return builder.create();
    }
  }

  @Override
  public int getCount() {
    return 4;
  }
}

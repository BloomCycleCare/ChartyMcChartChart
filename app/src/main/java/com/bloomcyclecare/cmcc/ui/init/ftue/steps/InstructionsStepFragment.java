package com.bloomcyclecare.cmcc.ui.init.ftue.steps;

import com.bloomcyclecare.cmcc.ui.instructions.InstructionsListFragment;
import com.stepstone.stepper.Step;
import com.stepstone.stepper.VerificationError;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class InstructionsStepFragment extends InstructionsListFragment implements Step {

  @Nullable
  @Override
  public VerificationError verifyStep() {
    return null;
  }

  @Override
  public void onSelected() {

  }

  @Override
  public void onError(@NonNull VerificationError error) {

  }
}

package com.bloomcyclecare.cmcc.ui.init.ftue.steps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.R;
import com.stepstone.stepper.Step;
import com.stepstone.stepper.VerificationError;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class StartStepFragment extends Fragment implements Step {

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_step_start, container, false);
  }

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

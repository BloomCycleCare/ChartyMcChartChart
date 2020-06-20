package com.bloomcyclecare.cmcc.ui.init.ftue;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.apps.charting.ViewMode;
import com.bloomcyclecare.cmcc.logic.PreferenceRepo;
import com.bloomcyclecare.cmcc.ui.main.MainActivity;
import com.stepstone.stepper.StepperLayout;
import com.stepstone.stepper.VerificationError;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class StepperFragment extends Fragment implements StepperLayout.StepperListener {

  StepperLayout mStepperLayout;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MainActivity mainActivity = (MainActivity) requireActivity();
    mainActivity.hideUp();
    setHasOptionsMenu(true);
  }

  @Override
  public void onPrepareOptionsMenu(@NonNull Menu menu) {
    super.onPrepareOptionsMenu(menu);
    menu.clear();
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_stepper, container, false);
    mStepperLayout = view.findViewById(R.id.stepperLayout);
    mStepperLayout.setAdapter(new StepAdapter(getChildFragmentManager(), requireContext()));
    mStepperLayout.setListener(this);

    return view;
  }

  @Override
  public void onCompleted(View completeButton) {
    PreferenceRepo preferenceRepo =
        ChartingApp.cast(requireActivity().getApplication()).preferenceRepo();
    ViewMode viewMode = preferenceRepo.currentSummary().defaultToDemoMode()
        ? ViewMode.DEMO : ViewMode.CHARTING;
    NavController navController = Navigation.findNavController(requireView());
    navController.navigate(StepperFragmentDirections.actionLoadChart().setViewMode(viewMode));
  }

  @Override
  public void onError(VerificationError verificationError) {

  }

  @Override
  public void onStepSelected(int newStepPosition) {

  }

  @Override
  public void onReturn() {

  }
}

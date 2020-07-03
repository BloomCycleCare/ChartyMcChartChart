package com.bloomcyclecare.cmcc.ui.init.ftue.steps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.models.instructions.Instructions;
import com.bloomcyclecare.cmcc.features.instructions.ui.InstructionSelectionFragment;
import com.stepstone.stepper.BlockingStep;
import com.stepstone.stepper.StepperLayout;
import com.stepstone.stepper.VerificationError;

import org.parceler.Parcels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class InstructionsStepFragment extends Fragment implements BlockingStep {

  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private InstructionsStepViewModel mViewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mViewModel = new ViewModelProvider(this).get(InstructionsStepViewModel.class);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_step_instructions, container, false);

    mDisposables.add(mViewModel.initialInstructions().observeOn(AndroidSchedulers.mainThread()).subscribe(initialInstructions -> {
      Bundle args = new Bundle();
      args.putParcelable(Instructions.class.getCanonicalName(), Parcels.wrap(initialInstructions));

      Fragment fragment = new InstructionSelectionFragment();
      fragment.setArguments(args);

      if (isAdded()) {
        getChildFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();
        getChildFragmentManager().executePendingTransactions();
      }
    }));

    return view;
  }

  @Override
  public void onNextClicked(StepperLayout.OnNextClickedCallback callback) {

  }

  @Override
  public void onCompleteClicked(StepperLayout.OnCompleteClickedCallback callback) {
    new AlertDialog.Builder(requireContext())
        .setTitle("Instruction Confirmation")
        .setMessage("I confirm this is my current set of instructions as provided by my FCP/FCPI.")
        .setPositiveButton("Confirm", (dialog, which) -> {
          mDisposables.add(mViewModel.commit()
              .subscribeOn(Schedulers.computation())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(() -> {
                callback.complete();
                dialog.dismiss();
              }));
        })
        .setNegativeButton("Cancel", (dialog, which) -> {
          Toast.makeText(requireContext(), "Please correct your selection", Toast.LENGTH_SHORT).show();
          dialog.dismiss();
        })
        .create()
        .show();
  }

  @Override
  public void onBackClicked(StepperLayout.OnBackClickedCallback callback) {

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

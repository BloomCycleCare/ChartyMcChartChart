package com.bloomcyclecare.cmcc.ui.init.ftue.steps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bloomcyclecare.cmcc.R;
import com.jakewharton.rxbinding2.widget.RxAdapterView;
import com.stepstone.stepper.Step;
import com.stepstone.stepper.VerificationError;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.joda.time.LocalDate;

import java.util.Calendar;
import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;

public class InitCycleStepFragment extends Fragment implements Step {

  private final CompositeDisposable mDisposables = new CompositeDisposable();

  private InitCycleViewModel mViewModel;

  private Button mDatePromptButton;
  private TextView mSelectionPromptText;
  private TextView mDateOutcomeText;
  private ImageView mOutcomeImageView;

  private Group mOuterSelectionGroup;
  private Group mSelectionActionGroup;
  private Group mSelectionOutcomeGroup;

  private volatile Optional<VerificationError> verificationError =
      Optional.of(new VerificationError("Still initializing"));

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mViewModel = new ViewModelProvider(this).get(InitCycleViewModel.class);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_step_cycle, container, false);

    mDatePromptButton = view.findViewById(R.id.button_date_prompt);
    mSelectionPromptText = view.findViewById(R.id.tv_selection_prompt);
    mDateOutcomeText = view.findViewById(R.id.tv_date_outcome);
    mOutcomeImageView = view.findViewById(R.id.iv_outcome);

    mOuterSelectionGroup = view.findViewById(R.id.selection_outer_group);
    mSelectionActionGroup = view.findViewById(R.id.selection_action_group);
    mSelectionOutcomeGroup = view.findViewById(R.id.selection_outcome_group);

    RxAdapterView.itemSelections(view.findViewById(R.id.spinner_cycle_type))
        .subscribe(mViewModel.mSpinnerSelectionSubject);

    mViewModel.viewState().observe(getViewLifecycleOwner(), this::render);
    return view;
  }

  private void render(InitCycleViewModel.ViewState viewState) {
    boolean showOutcome = viewState.hasCycle();
    boolean showActionGroup = !viewState.selectionPromptText().isEmpty();
    verificationError = viewState.verificationError();
    mSelectionActionGroup.setVisibility(showActionGroup ? View.VISIBLE : View.GONE);
    mSelectionOutcomeGroup.setVisibility(showOutcome ? View.VISIBLE : View.GONE);
    mOuterSelectionGroup.setVisibility(showOutcome ? View.GONE : View.VISIBLE);

    if (viewState.hasCycle()) {
      mDateOutcomeText.setText("Cycle created");
    }

    if (!viewState.selectionPromptText().isEmpty()) {
      mSelectionPromptText.setText(viewState.selectionPromptText());
      if (!viewState.hasCycle()) {
        mDatePromptButton.setOnClickListener(v -> {
          DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
              (view, year, monthOfYear, dayOfMonth) -> mDisposables.add(
                  mViewModel.createCycle(new LocalDate(year, monthOfYear + 1, dayOfMonth))
                      .subscribe()));
          datePickerDialog.setTitle(viewState.dateDialogTitle());
          datePickerDialog.setMaxDate(Calendar.getInstance());
          datePickerDialog.show(requireActivity().getFragmentManager(), "tag");
        });
      }
    }
  }

  @Nullable
  @Override
  public VerificationError verifyStep() {
    return verificationError.orElse(null);
  }

  @Override
  public void onSelected() {

  }

  @Override
  public void onError(@NonNull VerificationError error) {
    Toast.makeText(requireContext(), error.getErrorMessage(), Toast.LENGTH_SHORT).show();;
  }
}

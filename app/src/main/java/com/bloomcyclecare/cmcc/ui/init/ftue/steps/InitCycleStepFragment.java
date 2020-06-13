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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class InitCycleStepFragment extends Fragment implements Step {

  private InitCycleViewModel mViewModel;

  private Button mDatePromptButton;
  private TextView mSelectionPromptText;
  private TextView mDateOutcomeText;
  private ImageView mOutcomeImageView;

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
    mDatePromptButton.setVisibility(View.GONE);
    mSelectionPromptText = view.findViewById(R.id.tv_selection_prompt);
    mSelectionPromptText.setVisibility(View.GONE);
    mDateOutcomeText = view.findViewById(R.id.tv_date_outcome);
    mDateOutcomeText.setVisibility(View.GONE);
    mOutcomeImageView = view.findViewById(R.id.iv_outcome);
    mOutcomeImageView.setVisibility(View.GONE);

    RxAdapterView.itemSelections(view.findViewById(R.id.spinner_cycle_type))
        .subscribe(mViewModel.mSpinnerSelectionSubject);

    mViewModel.viewState().observe(getViewLifecycleOwner(), this::render);
    return view;
  }

  private void render(InitCycleViewModel.ViewState viewState) {
    verificationError = viewState.verificationError();
    if (viewState.selectionPromptText().isEmpty()) {
      mSelectionPromptText.setVisibility(View.GONE);
      mDatePromptButton.setVisibility(View.GONE);
    } else {
      mSelectionPromptText.setText(viewState.selectionPromptText());
      mSelectionPromptText.setVisibility(View.VISIBLE);
      if (viewState.hasCycle()) {
        mDateOutcomeText.setVisibility(View.VISIBLE);
        mDateOutcomeText.setText("Cycle created");
        mDatePromptButton.setVisibility(View.GONE);
        mOutcomeImageView.setVisibility(View.VISIBLE);
      } else {
        mDateOutcomeText.setVisibility(View.GONE);
        mDatePromptButton.setVisibility(View.VISIBLE);
        mDatePromptButton.setOnClickListener(v -> {
          DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
              (view, year, monthOfYear, dayOfMonth) -> mViewModel.setCycleDate(
                  new LocalDate(year, monthOfYear + 1, dayOfMonth)));
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

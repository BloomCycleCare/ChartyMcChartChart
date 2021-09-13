package com.bloomcyclecare.cmcc.ui.medication.detail;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ui.main.MainViewModel;
import com.jakewharton.rxbinding2.widget.RxTextView;

import java.util.Optional;

import io.reactivex.disposables.CompositeDisposable;

public class MedicationDetailFragment extends Fragment {

  private final CompositeDisposable mDisposable = new CompositeDisposable();

  private MainViewModel mMainViewModel;
  private MedicationDetailViewModel mMedicationDetailViewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    MedicationDetailFragmentArgs args = MedicationDetailFragmentArgs.fromBundle(requireArguments());
    MedicationDetailViewModel.Factory factory = new MedicationDetailViewModel.Factory(
        requireActivity().getApplication(), args);
    mMedicationDetailViewModel = new ViewModelProvider(this, factory)
        .get(MedicationDetailViewModel.class);
    mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

    requireActivity().getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
      @Override
      public void handleOnBackPressed() {
        onBackPressed();
      }
    });
  }

  private void onBackPressed() {
    mDisposable.add(mMedicationDetailViewModel.dirty().subscribe(isDirty -> {
      if (!isDirty) {
        Navigation.findNavController(requireView()).popBackStack();
      }
      new AlertDialog.Builder(requireContext())
          .setTitle("Save Changes?")
          .setMessage("Would you like to save your changes?")
          .setPositiveButton("Yes", (dialog, which) -> {
            doSave();
            dialog.dismiss();
          })
          .setNegativeButton("No", (dialog, which) -> {
            Navigation.findNavController(requireView()).popBackStack();
            dialog.dismiss();
          })
          .show();
    }));
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.menu_medication_detail, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;
      case R.id.action_save:
        doSave();
        return true;
      case R.id.action_delete:
        mDisposable.add(mMedicationDetailViewModel.delete().subscribe(() -> {
          Toast.makeText(requireContext(), "Medication deleted", Toast.LENGTH_SHORT).show();
          Navigation.findNavController(requireView()).popBackStack();
        }));
        return true;
    }
    return false;
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_medication_detail, container, false);

    TextView nameView = view.findViewById(R.id.tv_medication_name_value);
    TextView descriptionView = view.findViewById(R.id.tv_medication_description_value);
    TextView dosageView = view.findViewById(R.id.tv_medication_dosage_value);
    TextView frequencyView = view.findViewById(R.id.tv_medication_frequency_value);

    RxTextView.textChanges(nameView).map(CharSequence::toString)
        .subscribe(mMedicationDetailViewModel.nameSubject);
    RxTextView.textChanges(descriptionView).map(CharSequence::toString)
        .subscribe(mMedicationDetailViewModel.descriptionSubject);
    RxTextView.textChanges(dosageView).map(CharSequence::toString)
        .subscribe(mMedicationDetailViewModel.dosageSubject);
    RxTextView.textChanges(frequencyView).map(CharSequence::toString)
        .subscribe(mMedicationDetailViewModel.frequencySubject);

    mMedicationDetailViewModel.viewState().observe(getViewLifecycleOwner(), viewState -> {
      mMainViewModel.updateTitle(viewState.title);
      mMainViewModel.updateSubtitle(viewState.subtitle);

      maybeUpdate(nameView, viewState.medication.name);
      maybeUpdate(descriptionView, viewState.medication.description);
      maybeUpdate(dosageView, viewState.medication.dosage);
      maybeUpdate(frequencyView, viewState.medication.frequency);
    });
    return view;
  }

  private void doSave() {
    mDisposable.add(mMedicationDetailViewModel.save().subscribe(() -> {
      Toast.makeText(requireContext(), "Medication updated", Toast.LENGTH_SHORT).show();
      Navigation.findNavController(requireView()).popBackStack();
    }));
  }

  private static void maybeUpdate(TextView view, String value) {
    if (view.getText().toString().equals(value)) {
      return;
    }
    view.setText(value);
  }
}

package com.bloomcyclecare.cmcc.ui.medication.detail;

import android.app.AlertDialog;
import android.content.Context;
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
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ui.main.MainViewModel;
import com.jakewharton.rxbinding2.widget.RxTextView;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public class MedicationDetailFragment extends Fragment {

  private final CompositeDisposable mDisposable = new CompositeDisposable();

  private MainViewModel mMainViewModel;
  private MedicationDetailViewModel mMedicationDetailViewModel;

  @Override
  public void onDestroy() {
    mDisposable.clear();
    super.onDestroy();
  }

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
    NavController navController = Navigation.findNavController(requireView());
    Context context = requireContext();
    mDisposable.add(mMedicationDetailViewModel.dirty().observeOn(AndroidSchedulers.mainThread()).subscribe(isDirty -> {
      if (!isDirty) {
        navController.popBackStack();
        return;
      }
      new AlertDialog.Builder(context)
          .setTitle("Save Changes?")
          .setMessage("Would you like to save your changes?")
          .setPositiveButton("Yes", (dialog, which) -> {
            dialog.dismiss();
            doSave(context, navController);
          })
          .setNegativeButton("No", (dialog, which) -> {
            dialog.dismiss();
            navController.popBackStack();
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
        doSave(requireContext(), Navigation.findNavController(requireView()));
        return true;
      case R.id.action_delete:
        mDisposable.add(mMedicationDetailViewModel.delete().observeOn(AndroidSchedulers.mainThread()).subscribe(() -> {
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
    SwitchCompat activeView = view.findViewById(R.id.tv_medication_active_value);

    AtomicBoolean initialized = new AtomicBoolean();

    mMedicationDetailViewModel.viewState().observe(getViewLifecycleOwner(), viewState -> {
      Timber.d("Rendering ViewState");
      mMainViewModel.updateTitle(viewState.title);
      mMainViewModel.updateSubtitle(viewState.subtitle);

      maybeUpdate(nameView, viewState.medication.name);
      maybeUpdate(descriptionView, viewState.medication.description);
      maybeUpdate(dosageView, viewState.medication.dosage);
      maybeUpdate(frequencyView, viewState.medication.timesPerDay == 0 ? "" : String.valueOf(viewState.medication.timesPerDay));

      if (activeView.isChecked() != viewState.medication.active) {
        activeView.setChecked(viewState.medication.active);
      }

      if (initialized.compareAndSet(false, true)) {
        RxTextView.textChanges(nameView).map(CharSequence::toString)
            .subscribe(mMedicationDetailViewModel.nameSubject);
        RxTextView.textChanges(descriptionView).map(CharSequence::toString)
            .subscribe(mMedicationDetailViewModel.descriptionSubject);
        RxTextView.textChanges(dosageView).map(CharSequence::toString)
            .subscribe(mMedicationDetailViewModel.dosageSubject);
        RxTextView.textChanges(frequencyView).map(value -> {
          if (value.length() == 0) {
            return Optional.<Integer>empty();
          }
          return Optional.of(Integer.valueOf(value.toString()));
        }).subscribe(mMedicationDetailViewModel.timesPerDaySubject);
        //TODO: hook up switch
      }
    });
    return view;
  }

  private void doSave(Context context, NavController navController) {
    mDisposable.add(mMedicationDetailViewModel.save()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(() -> {
          Toast.makeText(context, "Medication updated", Toast.LENGTH_SHORT).show();
          navController.popBackStack();
        }));
  }

  private static void maybeUpdate(TextView view, String value) {
    if (view.getText().toString().equals(value)) {
      return;
    }
    view.setText(value);
  }
}

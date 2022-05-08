package com.bloomcyclecare.cmcc.ui.medication.prescription;

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

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class PrescriptionDetailFragment extends Fragment {

  private final CompositeDisposable mDisposable = new CompositeDisposable();

  private MainViewModel mMainViewModel;
  private PrescriptionDetailViewModel mFragmentViewModel;

  @Override
  public void onDestroy() {
    mDisposable.clear();
    super.onDestroy();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    PrescriptionDetailFragmentArgs args = PrescriptionDetailFragmentArgs.fromBundle(requireArguments());
    PrescriptionDetailViewModel.Factory factory = new PrescriptionDetailViewModel.Factory(
        requireActivity().getApplication(), args);
    mFragmentViewModel = new ViewModelProvider(this, factory)
        .get(PrescriptionDetailViewModel.class);
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
    if (!mFragmentViewModel.dirty()) {
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
        mDisposable.add(mFragmentViewModel.delete().observeOn(AndroidSchedulers.mainThread()).subscribe(() -> {
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
    View view = inflater.inflate(R.layout.fragment_prescription_detail, container, false);

    TextView dosageView = view.findViewById(R.id.tv_medication_dosage_value);
    SwitchCompat takeMorningSwitch = view.findViewById(R.id.take_in_morning_switch);
    SwitchCompat takeNoonSwitch = view.findViewById(R.id.take_at_noon_switch);
    SwitchCompat takeEveningSwitch = view.findViewById(R.id.take_in_evening_switch);
    SwitchCompat takeNightSwitch = view.findViewById(R.id.take_at_night_switch);
    SwitchCompat takeAsNeededSwitch = view.findViewById(R.id.take_as_needed_value);

    AtomicBoolean initialized = new AtomicBoolean();

    mFragmentViewModel.viewState().observe(getViewLifecycleOwner(), viewState -> {
      Timber.d("Rendering ViewState");
      mMainViewModel.updateTitle(viewState.title);
      mMainViewModel.updateSubtitle(viewState.subtitle);

      String dosage = viewState.prescription.dosage();
      if (dosage == null) {
        dosage = "";
      }
      maybeUpdate(dosageView, dosage);

      takeMorningSwitch.setChecked(viewState.prescription.takeInMorning());
      takeNoonSwitch.setChecked(viewState.prescription.takeAtNoon());
      takeEveningSwitch.setChecked(viewState.prescription.takeInEvening());
      takeNightSwitch.setChecked(viewState.prescription.takeAtNight());
      takeAsNeededSwitch.setChecked(viewState.prescription.takeAsNeeded());

      takeMorningSwitch.setEnabled(!viewState.prescription.takeAsNeeded());
      takeNoonSwitch.setEnabled(!viewState.prescription.takeAsNeeded());
      takeEveningSwitch.setEnabled(!viewState.prescription.takeAsNeeded());
      takeNightSwitch.setEnabled(!viewState.prescription.takeAsNeeded());

      if (initialized.compareAndSet(false, true)) {
        RxTextView.textChanges(dosageView).map(CharSequence::toString)
            .subscribe(mFragmentViewModel.dosageSubject);
        takeMorningSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
          mFragmentViewModel.takeMorningSubject.onNext(isChecked);
        });
        takeNoonSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
          mFragmentViewModel.takeNoonSubject.onNext(isChecked);
        });
        takeEveningSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
          mFragmentViewModel.takeEveningSubject.onNext(isChecked);
        });
        takeNightSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
          mFragmentViewModel.takeNightSubject.onNext(isChecked);
        });
        takeAsNeededSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
          mFragmentViewModel.takeAsNeededSubject.onNext(isChecked);
        });
      }
    });
    return view;
  }

  private void doSave(Context context, NavController navController) {
    mDisposable.add(mFragmentViewModel.save()
        .subscribeOn(Schedulers.computation())
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

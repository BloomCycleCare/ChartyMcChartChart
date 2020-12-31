package com.bloomcyclecare.cmcc.ui.entry;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.models.breastfeeding.BreastfeedingEntry;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;

import org.parceler.Parcels;

import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class BreastfeedingEntryFragment extends Fragment {

  private BreastfeedingEntry mInitialEntry;
  private EntryDetailViewModel mEntryViewModel;
  private BreastfeedingEntryViewModel mFragmentViewModel;

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    CycleRenderer.EntryModificationContext modificationContext =
        Parcels.unwrap(requireArguments().getParcelable(
            CycleRenderer.EntryModificationContext.class.getCanonicalName()));
    mInitialEntry = modificationContext.entry.breastfeedingEntry;

    BreastfeedingEntryViewModel.Factory factory =
        new BreastfeedingEntryViewModel.Factory(
            requireActivity().getApplication(),
            mInitialEntry);
    mFragmentViewModel =
        new ViewModelProvider(requireActivity(), factory).get(BreastfeedingEntryViewModel.class);
    mEntryViewModel = new ViewModelProvider(requireActivity()).get(EntryDetailViewModel.class);
  }

  private void configureNumberPicker(TextView tv, String title, int initialValue, Subject<Optional<Integer>> valueSubject) {
    tv.setOnClickListener(v -> {
      NumberPicker numberPicker = new NumberPicker(requireContext());
      numberPicker.setMinValue(0);
      numberPicker.setMaxValue(30);
      numberPicker.setValue(initialValue);

      new AlertDialog.Builder(requireContext())
          .setTitle(title)
          .setView(numberPicker)
          .setPositiveButton("Confirm", (d,w) -> {
            valueSubject.onNext(Optional.of(numberPicker.getValue()));
            d.dismiss();
          })
          .setNegativeButton("Cancel", (d,w) -> d.dismiss())
          .setNegativeButton("Clear", (d,w) -> {
            valueSubject.onNext(Optional.empty());
            d.dismiss();
          })
          .show();
    });
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_breastfeeding_entry, container, false);

    TextView maxGapValue = view.findViewById(R.id.max_gap_value);
    TextView numFeedingsNightValue = view.findViewById(R.id.night_feedings_value);
    TextView numFeedingsDayValue = view.findViewById(R.id.day_feedings_value);

    configureNumberPicker(
        numFeedingsDayValue,
        "Number of Feedings",
        Math.max(mInitialEntry.numDayFeedings, 0),
        mFragmentViewModel.numDadyFeedingsSubject);
    configureNumberPicker(
        numFeedingsNightValue,
        "Number of Feedings",
        Math.max(mInitialEntry.numNightFeedings, 0),
        mFragmentViewModel.numNightFeedingsSubject);

    mFragmentViewModel.entries().subscribe(mEntryViewModel.breastfeedingEntrySubject);

    mFragmentViewModel.viewStates().observe(getViewLifecycleOwner(), viewState -> {
      Timber.v("Rendering ViewState");
      if (viewState.entry.numDayFeedings < 0) {
        numFeedingsDayValue.setText("TBD");
      } else {
        numFeedingsDayValue.setText(String.valueOf(viewState.entry.numDayFeedings));
      }
      if (viewState.entry.numNightFeedings < 0) {
        numFeedingsNightValue.setText("TBD");
      } else {
        numFeedingsNightValue.setText(String.valueOf(viewState.entry.numNightFeedings));
      }
    });

    return view;
  }

}

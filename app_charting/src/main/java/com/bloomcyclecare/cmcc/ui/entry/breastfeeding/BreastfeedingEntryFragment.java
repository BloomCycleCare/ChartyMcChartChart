package com.bloomcyclecare.cmcc.ui.entry.breastfeeding;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.data.models.breastfeeding.BreastfeedingEntry;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.ui.entry.EntryDetailViewModel;

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

    view.findViewById(R.id.button_bbdb).setOnClickListener(v -> new AlertDialog.Builder(requireContext())
        .setTitle("Import from Baby Daybook")
        .setMessage("CMCC supports ingesting breastfeeding data recorded in the Baby Daybook app (which is great BTW). Please follow these steps to import your data:\n 1. Open Baby Daybook\n 2. Open Settings\n 3. Select Backup & Restore\n 4. Select the share button on the most recent backup (you can create a new one if you like)\n 5. Open the file with CMCC\n 6. Follow the onscreen instructions")
        .setPositiveButton("Open Baby Daybook", (d,w) -> {
          Intent intent = new Intent(Intent.ACTION_VIEW);
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          intent.setData(Uri.parse("market://details?id=" + "com.drillyapps.babydaybook"));
          startActivity(intent);
          d.dismiss();
        })
        .setNegativeButton("Close", (d,w) -> d.dismiss())
        .show());

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
      if (viewState.entry.maxGapBetweenFeedings == null) {
        maxGapValue.setText("TBD");
      } else {
        maxGapValue.setText(String.format("%.1f hrs", viewState.entry.maxGapBetweenFeedings.getStandardMinutes() / (float) 60));
      }
    });

    return view;
  }

}

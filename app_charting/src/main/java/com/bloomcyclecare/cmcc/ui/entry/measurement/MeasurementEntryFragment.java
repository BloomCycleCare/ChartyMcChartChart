package com.bloomcyclecare.cmcc.ui.entry.measurement;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.ui.entry.EntryDetailViewModel;
import com.jakewharton.rxbinding2.widget.RxAdapterView;

import org.jetbrains.annotations.NotNull;
import org.parceler.Parcels;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import timber.log.Timber;

public class MeasurementEntryFragment extends Fragment {

  private EntryDetailViewModel mEntryViewModel;
  private MeasurementEntryViewModel mViewModel;

  @Override
  public void onAttach(@NotNull Context context) {
    super.onAttach(context);

    CycleRenderer.EntryModificationContext modificationContext =
        Parcels.unwrap(requireArguments().getParcelable(
            CycleRenderer.EntryModificationContext.class.getCanonicalName()));
    MeasurementEntryViewModel.Factory factory =
        new MeasurementEntryViewModel.Factory(
            requireActivity().getApplication(),
            modificationContext.entry.measurementEntry);
    mViewModel =
        new ViewModelProvider(requireActivity(), factory).get(MeasurementEntryViewModel.class);

    mEntryViewModel = new ViewModelProvider(requireActivity()).get(EntryDetailViewModel.class);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_marquette_entry, container, false);

    // Set up the views
    Spinner monitorReadingSpinner = view.findViewById(R.id.monitor_reading_value);
    ArrayAdapter<CharSequence> monitorReadingAdapter = ArrayAdapter.createFromResource(
        requireActivity(), R.array.monitor_reading, android.R.layout.simple_spinner_item);
    monitorReadingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    monitorReadingSpinner.setAdapter(monitorReadingAdapter);
    View monitorReadingGroup = view.findViewById(R.id.monitor_reading_group);

    Spinner lhResultSpinner = view.findViewById(R.id.lh_result_value);
    ArrayAdapter<CharSequence> lhResultAdapter = ArrayAdapter.createFromResource(
        requireActivity(), R.array.lh_result, android.R.layout.simple_spinner_item);
    lhResultAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    lhResultSpinner.setAdapter(lhResultAdapter);
    View lhResultGroup = view.findViewById(R.id.lh_result_group);

    // Render view state updates from model
    AtomicBoolean viewStateRendered = new AtomicBoolean();
    mViewModel.viewState().observe(getViewLifecycleOwner(), viewState -> {
      View containerView = view.findViewById(R.id.monitor_reading_container);
      containerView.setBackgroundResource(viewState.monitorReadingBackgroundColor());

      TextView itemView = (TextView) monitorReadingSpinner.getChildAt(0);
      itemView.setTextColor(viewState.monitorReadingTextColor());

      if (monitorReadingSpinner.getSelectedItemPosition() != viewState.monitorReadingPosition()) {
        Timber.v("Updating monitor reading spinner to %s", viewState.monitorReadingPosition());
        monitorReadingSpinner.setSelection(viewState.monitorReadingPosition());
      }
      if (lhResultSpinner.getSelectedItemPosition() != viewState.lhTestResultPosition()) {
        Timber.v("Updating lh test result spinner to %s", viewState.lhTestResultPosition());
        lhResultSpinner.setSelection(viewState.lhTestResultPosition());
      }

      monitorReadingGroup.setVisibility(viewState.showMonitorReading() ? View.VISIBLE : View.GONE);
      lhResultGroup.setVisibility(viewState.showLHTestResult() ? View.VISIBLE : View.GONE);

      // Connect views to view model
      if (viewStateRendered.compareAndSet(false, true)) {
        RxAdapterView.itemSelections(monitorReadingSpinner).subscribe(mViewModel.monitorReadings);
        RxAdapterView.itemSelections(lhResultSpinner).subscribe(mViewModel.lhTestResults);
      }
    });

    // Connect results to entry view model
    mViewModel.measurements().subscribe(mEntryViewModel.measurementEntries);

    return view;
  }
}

package com.bloomcyclecare.cmcc.ui.entry;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.bloomcyclecare.cmcc.R;
import com.jakewharton.rxbinding2.widget.RxAdapterView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class MarquetteEntryFragment extends Fragment {

  private MarquetteEntryViewModel mViewModel;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    mViewModel =
        new ViewModelProvider(requireActivity()).get(MarquetteEntryViewModel.class);
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

    Spinner lhResultSpinner = view.findViewById(R.id.lh_result_value);
    ArrayAdapter<CharSequence> lhResultAdapter = ArrayAdapter.createFromResource(
        requireActivity(), R.array.lh_result, android.R.layout.simple_spinner_item);
    lhResultAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    lhResultSpinner.setAdapter(lhResultAdapter);

    // Connect views to view model
    RxAdapterView.itemSelections(monitorReadingSpinner).subscribe(mViewModel.monitorReadings);

    // Render view state updates from model
    mViewModel.viewState().observe(getViewLifecycleOwner(), viewState -> {
      View containerView = view.findViewById(R.id.monitor_reading_container);
      containerView.setBackgroundResource(viewState.monitorReadingBackgroundColor());

      TextView itemView = (TextView) monitorReadingSpinner.getChildAt(0);
      itemView.setTextColor(viewState.monitorReadingTextColor());
    });

    return view;
  }
}

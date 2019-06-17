package com.roamingroths.cmcc.ui.entry.detail;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.PreferenceManager;

import com.google.common.base.Strings;
import com.jakewharton.rxbinding2.widget.RxAdapterView;
import com.jakewharton.rxbinding2.widget.RxCompoundButton;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.domain.IntercourseTimeOfDay;
import com.roamingroths.cmcc.data.domain.Observation;
import com.roamingroths.cmcc.data.entities.ObservationEntry;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.functions.Action;
import timber.log.Timber;

/**
 * Created by parkeroth on 9/11/17.
 */

public class ObservationEntryFragment extends Fragment {

  public static final int OK_RESPONSE = 0;

  private EntryDetailViewModel mEntryDetailViewModel;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    mEntryDetailViewModel = ViewModelProviders.of(getActivity()).get(EntryDetailViewModel.class);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_chart_entry, container, false);

    TextView observationDescriptionTextView =
        view.findViewById(R.id.tv_modify_observation_description);
    EditText observationEditText = view.findViewById(R.id.et_modify_observation_value);
    Switch unusualBleedingSwitch = view.findViewById(R.id.switch_unusual_bleeding);
    Switch peakDaySwitch = view.findViewById(R.id.switch_peak_day);
    Switch intercourseSwitch = view.findViewById(R.id.switch_intercourse);
    Switch essentialSamenessSwitch = view.findViewById(R.id.switch_essential_sameness);
    Switch firstDaySwitch = view.findViewById(R.id.switch_new_cycle);
    Switch pointOfChangeSwitch = view.findViewById(R.id.switch_point_of_change);
    Spinner intercourseSpinner = view.findViewById(R.id.spinner_intercourse);

    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
        R.array.intercourse_times_of_day, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    intercourseSpinner.setAdapter(adapter);
    intercourseSpinner.setVisibility(View.GONE);

    View essentialSamenessLayout = view.findViewById(R.id.essential_sameness_layout);
    View pointOfChangeLayout = view.findViewById(R.id.point_of_change_layout);
    View unusualBleedingLayout = view.findViewById(R.id.unusual_bleeding_layout);
    View firstDayLayout = view.findViewById(R.id.layout_new_cycle);

    Action connectStreams = () -> {
      Timber.d("Connecting RX streams for UI updates");
      RxCompoundButton
          .checkedChanges(unusualBleedingSwitch)
          .subscribe(mEntryDetailViewModel.unusualBleedingUpdates);
      RxCompoundButton
          .checkedChanges(peakDaySwitch)
          .subscribe(mEntryDetailViewModel.peakDayUpdates);
      RxCompoundButton
          .checkedChanges(intercourseSwitch)
          .subscribe(mEntryDetailViewModel.intercourseUpdates);
      RxCompoundButton
          .checkedChanges(essentialSamenessSwitch)
          .subscribe(mEntryDetailViewModel.isEssentiallyTheSameUpdates);
      RxCompoundButton
          .checkedChanges(firstDaySwitch)
          .subscribe(mEntryDetailViewModel.firstDayOfCycleUpdates);
      RxCompoundButton
          .checkedChanges(pointOfChangeSwitch)
          .subscribe(mEntryDetailViewModel.pointOfChangeUpdates);

      RxAdapterView.itemSelections(intercourseSpinner)
          .map(index -> IntercourseTimeOfDay.values()[index])
          .subscribe(mEntryDetailViewModel.timeOfDayUpdates);

      RxTextView.afterTextChangeEvents(observationEditText)
          .map(event -> event.view().getText().toString())
          .subscribe(mEntryDetailViewModel.observationUpdates);
    };

    final AtomicBoolean uiInitialized = new AtomicBoolean(false);
    mEntryDetailViewModel.viewStates().observe(this, (viewState -> {
      Timber.d("Updating ViewState");
      if (viewState.entryContext.isFirstEntry && !viewState.entryContext.hasPreviousCycle) {
        firstDayLayout.setVisibility(View.GONE);
      }
      ObservationEntry observationEntry = viewState.chartEntry.observationEntry;
      if (!Strings.isNullOrEmpty(viewState.observationErrorText)) {
        Timber.d("Found invalid observation");
        String existingErrorText = observationEditText.getError() == null
            ? "" : observationEditText.getError().toString();
        if (!existingErrorText.equals(viewState.observationErrorText)) {
          observationEditText.setError(viewState.observationErrorText);
          observationDescriptionTextView.setText(null);
        } else {
          Timber.v("Error text already updated");
        }
      } else {
        Timber.d("Found valid observation");
        observationEditText.setError(null);
        Observation observation = observationEntry.observation;
        if (observation != null) {
          String newText = observation.toString();
          String existingText = observationEditText.getText() == null ? null : observationEditText.getText().toString();
          if (existingText == null || existingText.equals(newText)) {
            observationEditText.setText(newText);
            observationDescriptionTextView.setText(observation.getDescription());
          }
          observationEditText.setText(observation.toString());
          observationDescriptionTextView.setText(observation.getDescription());
        }
        if (observation != null && observation.hasBlood()) {
          unusualBleedingLayout.setVisibility(View.VISIBLE);
        } else {
          unusualBleedingLayout.setVisibility(View.GONE);
        }
        if (observation != null
            && observation.dischargeSummary != null
            && observation.dischargeSummary.isPeakType()
            && viewState.entryContext.shouldAskEssentialSameness) {
          essentialSamenessLayout.setVisibility(View.VISIBLE);
        } else {
          essentialSamenessLayout.setVisibility(View.GONE);
        }
      }
      if (observationEntry.intercourseTimeOfDay != IntercourseTimeOfDay.NONE) {
        intercourseSpinner.setSelection(observationEntry.intercourseTimeOfDay.ordinal());
      }
      if (observationEntry.intercourse) {
        intercourseSpinner.setVisibility(View.VISIBLE);
      } else {
        intercourseSpinner.setVisibility(View.GONE);
      }
      maybeUpdate(intercourseSwitch, observationEntry.intercourse);
      maybeUpdate(peakDaySwitch, observationEntry.peakDay);
      maybeUpdate(firstDaySwitch, observationEntry.firstDay);
      maybeUpdate(pointOfChangeSwitch, observationEntry.pointOfChange);
      maybeUpdate(unusualBleedingSwitch, observationEntry.unusualBleeding);
      maybeUpdate(essentialSamenessSwitch, observationEntry.isEssentiallyTheSame);

      SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
      boolean usingPrePeakYellowStickers = preferences.getBoolean("enable_pre_peak_yellow_stickers", false);
      pointOfChangeLayout.setVisibility(usingPrePeakYellowStickers ? View.VISIBLE : View.GONE);

      if (uiInitialized.compareAndSet(false, true)) {
        try {
          connectStreams.run();
        } catch (Exception e) {
          Timber.e(e);
        }
      }
    }));

    return view;
  }

  private static void maybeUpdate(Switch view, boolean value) {
    if (view.isChecked() != value) {
      Timber.d("Updating %s", view.getResources().getResourceEntryName(view.getId()));
      view.setChecked(value);
    }
  }
}

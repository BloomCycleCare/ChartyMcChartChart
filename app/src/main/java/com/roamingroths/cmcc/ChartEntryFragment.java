package com.roamingroths.cmcc;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.data.ChartEntryProvider;
import com.roamingroths.cmcc.data.EntryProvider;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Observation;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.Listeners;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by parkeroth on 9/11/17.
 */

public class ChartEntryFragment extends EntryFragment<ChartEntry> {

  public static final int OK_RESPONSE = 0;

  private TextInputEditText mObservationEditText;
  private TextView mObservationDescriptionTextView;
  private Switch mPeakDaySwitch;
  private Switch mIntercourseSwitch;
  private Switch mFirstDaySwitch;
  private Switch mPointOfChangeSwitch;
  private View mUnusualBleedingLayout;
  private Switch mUnusualBleedingSwitch;
  private View mPointOfChangeLayout;

  private boolean expectUnusualBleeding;
  private boolean usingPrePeakYellowStickers;

  public ChartEntryFragment() {
    super(ChartEntry.class, "ChartEntryFragment", R.layout.fragment_chart_entry);
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
  }

  @Override
  EntryProvider<ChartEntry> createEntryProvider(FirebaseDatabase db) {
    return ChartEntryProvider.forDb(db);
  }

  @Override
  void duringCreateView(View view, Bundle args, Bundle savedInstanceState) {
    expectUnusualBleeding = args.getBoolean(Extras.EXPECT_UNUSUAL_BLEEDING);

    mPointOfChangeLayout = view.findViewById(R.id.point_of_change_layout);

    mUnusualBleedingLayout = view.findViewById(R.id.unusual_bleeding_layout);
    mUnusualBleedingSwitch = (Switch) view.findViewById(R.id.switch_unusual_bleeding);

    mObservationDescriptionTextView =
        (TextView) view.findViewById(R.id.tv_modify_observation_description);

    mObservationEditText = (TextInputEditText) view.findViewById(R.id.et_modify_observation_value);
    mObservationEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
          try {
            updateUiWithObservation(getObservationFromView(v));
          } catch (Observation.InvalidObservationException ioe) {
            mObservationEditText.setError(ioe.getMessage());
          }
        }
      }
    });
    mObservationEditText.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        try {
          Observation observation = getObservationFromEditText();
          if (observation != null) {
            mObservationDescriptionTextView.setText(observation.getMultiLineDescription());
          }
          if (observation != null && observation.hasBlood()) {
            mUnusualBleedingLayout.setVisibility(View.VISIBLE);
          } else {
            mUnusualBleedingLayout.setVisibility(View.GONE);
          }
        } catch (Observation.InvalidObservationException ioe) {
          mObservationDescriptionTextView.setText(null);
        }
      }

      @Override
      public void afterTextChanged(Editable s) {
      }
    });

    mPeakDaySwitch = (Switch) view.findViewById(R.id.switch_peak_day);
    mIntercourseSwitch = (Switch) view.findViewById(R.id.switch_intercourse);
    mFirstDaySwitch = (Switch) view.findViewById(R.id.switch_new_cycle);
    mPointOfChangeSwitch = (Switch) view.findViewById(R.id.switch_point_of_change);
  }

  @Override
  public void onResume() {
    super.onResume();
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    usingPrePeakYellowStickers = preferences.getBoolean("enable_pre_peak_yellow_stickers", false);
    mPointOfChangeLayout.setVisibility(usingPrePeakYellowStickers ? View.VISIBLE : View.GONE);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    // TODO
  }

  @Override
  public Set<ValidationIssue> validateEntry(ChartEntry entry) {
    Set<ValidationIssue> issues = new HashSet<>();
    boolean entryHasBlood = entry.observation != null && entry.observation.hasBlood();
    if (entryHasBlood && expectUnusualBleeding && !entry.unusualBleeding) {
      issues.add(new ValidationIssue("Unusual bleeding?", "Are yousure this bleedin is typical?"));
    }
    return issues;
  }

  public void onDelete(final Callbacks.Callback<Void> onDone) {
    getEntryProvider().deleteEntry(getCycle().id, getEntryDate(), Listeners.doneOnCompletion(onDone));
  }

  public boolean shouldJoinCycle() {
    try {
      return getExistingEntry().firstDay && !getEntryFromUi().firstDay;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean shouldSplitCycle() {
    try {
      return !getExistingEntry().firstDay && getEntryFromUi().firstDay;
    } catch (Exception e) {
      return false;
    }
  }

  private Observation getObservationFromView(View v)
      throws Observation.InvalidObservationException {
    TextView tv = (TextView) v;
    String observationStr = tv.getText().toString();
    return Observation.fromString(observationStr);
  }

  private Observation getObservationFromEditText()
      throws Observation.InvalidObservationException {
    return getObservationFromView(mObservationEditText);
  }

  @Override
  public ChartEntry getEntryFromUi() throws Exception {
    Observation observation = getObservationFromEditText();
    boolean peakDay = mPeakDaySwitch.isChecked();
    boolean intercourse = mIntercourseSwitch.isChecked();
    boolean firstDay = mFirstDaySwitch.isChecked();
    boolean pointOfChange = mPointOfChangeSwitch.isChecked();
    boolean unusualBleeding = mUnusualBleedingSwitch.isChecked();
    return new ChartEntry(
        getEntryDate(), observation, peakDay, intercourse, firstDay, pointOfChange, unusualBleeding, getCycle().keys.chartKey);
  }

  @Override
  void updateUiWithEntry(ChartEntry entry) {
    updateUiWithObservation(entry.observation);
    mPeakDaySwitch.setChecked(entry.peakDay);
    mIntercourseSwitch.setChecked(entry.intercourse);
    mFirstDaySwitch.setChecked(entry.firstDay);
    mPointOfChangeSwitch.setChecked(entry.pointOfChange);
    mUnusualBleedingSwitch.setChecked(entry.unusualBleeding);
    if (entry.observation == null || !entry.observation.hasBlood()) {
      mUnusualBleedingLayout.setVisibility(View.GONE);
    } else {
      mUnusualBleedingLayout.setVisibility(View.VISIBLE);
    }
  }

  private void updateUiWithObservation(@Nullable Observation observation) {
    if (observation != null) {
      mObservationDescriptionTextView.setText(observation.getMultiLineDescription());
      mObservationEditText.setText(observation.toString());
      mObservationEditText.setError(null);
    }
  }
}

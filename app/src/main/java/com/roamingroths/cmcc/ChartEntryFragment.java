package com.roamingroths.cmcc;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Strings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.data.ChartEntryProvider;
import com.roamingroths.cmcc.data.CycleProvider;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.Observation;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

/**
 * Created by parkeroth on 9/11/17.
 */

public class ChartEntryFragment extends EntryFragment {

  public static final int OK_RESPONSE = 0;

  private ChartEntryProvider chartEntryProvider;
  private CycleProvider cycleProvider;

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
  private Cycle mCycle;
  private LocalDate mEntryDate;
  private ChartEntry mExistingEntry;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    chartEntryProvider = ChartEntryProvider.forDb(FirebaseDatabase.getInstance());
    cycleProvider = CycleProvider.forDb(FirebaseDatabase.getInstance());

  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_chart_entry, container, false);

    Bundle args = getArguments();
    mCycle = args.getParcelable(Cycle.class.getName());
    expectUnusualBleeding = args.getBoolean(Extras.EXPECT_UNUSUAL_BLEEDING);
    String entryDateStr = args.getString(Extras.ENTRY_DATE_STR);
    mEntryDate = DateUtil.fromWireStr(entryDateStr);

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

    chartEntryProvider.getChartEntry(mCycle.id, entryDateStr, mCycle.keys.chartKey, new Callbacks.Callback<ChartEntry>() {
      @Override
      public void acceptData(ChartEntry data) {
        mExistingEntry = data;
        updateUiWithEntry(mExistingEntry);
      }

      @Override
      public void handleNotFound() {
        throw new IllegalStateException("Could not load ChartEntry");
      }

      @Override
      public void handleError(DatabaseError error) {
        error.toException().printStackTrace();
      }
    });

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    usingPrePeakYellowStickers = preferences.getBoolean("enable_pre_peak_yellow_stickers", false);
    mPointOfChangeLayout.setVisibility(usingPrePeakYellowStickers ? View.VISIBLE : View.GONE);
  }

  public boolean isDirty() {
    if (mExistingEntry == null) {
      return false;
    }
    try {
      ChartEntry entryFromUi = getChartEntryFromUi();
      return !mExistingEntry.equals(entryFromUi);
    } catch (Observation.InvalidObservationException ioe) {
      return true;
    }
  }

  public void onSave() {
    try {
      final ChartEntry entry = getChartEntryFromUi();
      boolean entryHasBlood = entry.observation != null && entry.observation.hasBlood();
      if (entryHasBlood && expectUnusualBleeding && !entry.unusualBleeding) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Unusual bleeding?");
        builder.setMessage("Are you sure this bleeding is typical?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            doSaveAction(entry);
          }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
          }
        });
        builder.create().show();
      }
      doSaveAction(entry);
    } catch (Observation.InvalidObservationException ioe) {
      Toast.makeText(getActivity(), "Cannot save invalid observation", Toast.LENGTH_LONG).show();
    }
  }

  public void onDelete() {
    DatabaseReference.CompletionListener listener = new DatabaseReference.CompletionListener() {
      @Override
      public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
        getActivity().finish();
      }
    };
    chartEntryProvider.deleteChartEntry(mCycle.id, mEntryDate, listener);
  }

  private void doSaveAction(final ChartEntry entry) {
    if (mExistingEntry.firstDay != entry.firstDay) {
      maybeSplitOrJoinCycle(entry);
    } else {
      try {
        chartEntryProvider.putEntry(mCycle.id, entry, completionListener(mCycle));
      } catch (CryptoUtil.CryptoException ce) {
        ce.printStackTrace();
      }
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

  private ChartEntry getChartEntryFromUi() throws Observation.InvalidObservationException {
    Observation observation = getObservationFromEditText();
    boolean peakDay = mPeakDaySwitch.isChecked();
    boolean intercourse = mIntercourseSwitch.isChecked();
    boolean firstDay = mFirstDaySwitch.isChecked();
    boolean pointOfChange = mPointOfChangeSwitch.isChecked();
    boolean unusualBleeding = mUnusualBleedingSwitch.isChecked();
    return new ChartEntry(
        mEntryDate, observation, peakDay, intercourse, firstDay, pointOfChange, unusualBleeding, mCycle.keys.chartKey);
  }

  private void maybeSplitOrJoinCycle(final ChartEntry entry) {
    final Intent returnIntent = new Intent();
    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    if (entry.firstDay) {
      // Try and split the current cycle
      cycleProvider.splitCycle(userId, mCycle, entry, new Callbacks.HaltingCallback<Cycle>() {
        @Override
        public void acceptData(Cycle newCycle) {
          try {
            chartEntryProvider.putEntry(newCycle.id, entry, completionListener(newCycle));
          } catch (CryptoUtil.CryptoException ce) {
            ce.printStackTrace();
          }
        }
      });
    } else {
      // Try and this and the previous cycle
      if (Strings.isNullOrEmpty(mCycle.previousCycleId)) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("No Previous Cycle");
        builder.setMessage("Please add cycle before this entry to proceed.");
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            mFirstDaySwitch.setChecked(true);
            dialog.dismiss();
          }
        });
        builder.create().show();
      } else {
        cycleProvider.combineCycles(mCycle, userId, new Callbacks.HaltingCallback<Cycle>() {
          @Override
          public void acceptData(Cycle newCycle) {
            try {
              chartEntryProvider.putEntry(newCycle.id, entry, completionListener(newCycle));
            } catch (CryptoUtil.CryptoException ce) {
              ce.printStackTrace();
            }
          }
        });
      }
    }
  }

  private DatabaseReference.CompletionListener completionListener(final Cycle newCycle) {
    return new DatabaseReference.CompletionListener() {
      @Override
      public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
        if (databaseError != null) {
          databaseError.toException().printStackTrace();
          return;
        }
        Intent returnIntent = new Intent();
        returnIntent.putExtra(Cycle.class.getName(), newCycle);
        getActivity().setResult(OK_RESPONSE, returnIntent);
        getActivity().finish();
      }
    };
  }

  private void updateUiWithEntry(ChartEntry entry) {
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

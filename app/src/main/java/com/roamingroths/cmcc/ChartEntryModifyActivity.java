package com.roamingroths.cmcc;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Strings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.roamingroths.cmcc.data.ChartEntry;
import com.roamingroths.cmcc.data.Cycle;
import com.roamingroths.cmcc.data.DataStore;
import com.roamingroths.cmcc.data.Observation;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.CryptoUtil;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

public class ChartEntryModifyActivity extends AppCompatActivity {

  private static final String UI_DATE_FORMAT = "EEE, d MMM yyyy";

  public static final int CREATE_REQUEST = 1;
  public static final int MODIFY_REQUEST = 2;

  public static final int OK_RESPONSE = 0;
  public static final int CANCEL_RESPONSE = 1;

  private TextInputEditText mObservationEditText;
  private TextView mObservationDescriptionTextView;
  private Switch mPeakDaySwitch;
  private Switch mIntercourseSwitch;
  private Switch mFirstDaySwitch;
  private Switch mPointOfChangeSwitch;
  private View mUnusualBleedingLayout;
  private Switch mUnusualBleedingSwitch;

  private boolean expectUnusualBleeding;
  private boolean usingPrePeakYellowStickers;
  private Cycle mCycle;
  private LocalDate mEntryDate;
  private ChartEntry mExistingEntry;

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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_observation_modify);

    mUnusualBleedingLayout = findViewById(R.id.unusual_bleeding_layout);
    mUnusualBleedingSwitch = (Switch) findViewById(R.id.switch_unusual_bleeding);

    mObservationDescriptionTextView =
        (TextView) findViewById(R.id.tv_modify_observation_description);

    mObservationEditText = (TextInputEditText) findViewById(R.id.et_modify_observation_value);
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
      public void afterTextChanged(Editable s) {}
    });

    mPeakDaySwitch = (Switch) findViewById(R.id.switch_peak_day);
    mIntercourseSwitch = (Switch) findViewById(R.id.switch_intercourse);
    mFirstDaySwitch = (Switch) findViewById(R.id.switch_new_cycle);
    mPointOfChangeSwitch = (Switch) findViewById(R.id.switch_point_of_change);

    Intent intent = getIntent();
    if (!intent.hasExtra(Cycle.class.getName())) {
      throw new IllegalStateException("Missing Cycle");
    }
    mCycle = intent.getParcelableExtra(Cycle.class.getName());
    if (!intent.hasExtra(Extras.ENTRY_DATE_STR)) {
      throw new IllegalStateException("Missing entry date");
    }
    String entryDateStr = intent.getStringExtra(Extras.ENTRY_DATE_STR);
    mEntryDate = DateUtil.fromWireStr(entryDateStr);
    expectUnusualBleeding = intent.getBooleanExtra(Extras.EXPECT_UNUSUAL_BLEEDING, false);

    DataStore.getChartEntry(this, mCycle.id, entryDateStr, new Callbacks.Callback<ChartEntry>() {
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

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle(mEntryDate.toString(UI_DATE_FORMAT));
  }

  @Override
  protected void onResume() {
    super.onResume();
    View pointOfChangeLayout = findViewById(R.id.point_of_change_layout);
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    usingPrePeakYellowStickers = preferences.getBoolean("enable_pre_peak_yellow_stickers", false);
    pointOfChangeLayout.setVisibility(usingPrePeakYellowStickers ? View.VISIBLE : View.GONE);
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
        mEntryDate, observation, peakDay, intercourse, firstDay, pointOfChange, unusualBleeding);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_observation_modify, menu);
    return true;
  }

  private void maybeSplitOrJoinCycle(final ChartEntry entry) {
    final Intent returnIntent = new Intent();
    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    if (entry.firstDay) {
      // Try and split the current cycle
      DataStore.splitCycle(this, userId, mCycle, entry, new Callbacks.HaltingCallback<Cycle>() {
        @Override
        public void acceptData(Cycle newCycle) {
          try {
            DataStore.putChartEntry(ChartEntryModifyActivity.this, newCycle.id, entry);
            returnIntent.putExtra(Cycle.class.getName(), newCycle);
            setResult(OK_RESPONSE, returnIntent);
            finish();
          } catch (CryptoUtil.CryptoException ce) {
            ce.printStackTrace();
          }
        }
      });
    } else {
      // Try and this and the previous cycle
      if (Strings.isNullOrEmpty(mCycle.previousCycleId)) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
        DataStore.combineCycles(mCycle, userId, new Callbacks.HaltingCallback<Cycle>() {
          @Override
          public void acceptData(Cycle newCycle) {
            try {
              DataStore.putChartEntry(ChartEntryModifyActivity.this, newCycle.id, entry);
              returnIntent.putExtra(Cycle.class.getName(), newCycle);
              setResult(OK_RESPONSE, returnIntent);
              finish();
            } catch (CryptoUtil.CryptoException ce) {
              ce.printStackTrace();
            }
          }
        });
      }
    }
  }

  private void doSaveAction(final ChartEntry entry) {
    if (mExistingEntry.firstDay != entry.firstDay) {
      maybeSplitOrJoinCycle(entry);
    } else {
      Intent returnIntent = new Intent();
      try {
        DataStore.putChartEntry(this, mCycle.id, entry);
        setResult(OK_RESPONSE, returnIntent);
        finish();
      } catch (CryptoUtil.CryptoException ce) {
        ce.printStackTrace();
      }
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    if (id == R.id.action_settings) {
      Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
      startActivity(startSettingsActivity);
      return true;
    }

    if (id == R.id.action_save) {
      try {
        final ChartEntry entry = getChartEntryFromUi();
        boolean entryHasBlood = entry.observation != null && entry.observation.hasBlood();
        if (entryHasBlood && expectUnusualBleeding && !entry.unusualBleeding) {
          AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
          return false;
        }
        doSaveAction(entry);
      } catch (Observation.InvalidObservationException ioe) {
        Toast.makeText(this, "Cannot save invalid observation", Toast.LENGTH_LONG).show();
      }
      return true;
    }

    if (id == R.id.action_delete) {
      new AlertDialog.Builder(this)
          //set message, title, and icon
          .setTitle("Delete")
          .setMessage("Do you want to permanently delete this entry?")
          .setIcon(R.drawable.ic_delete_forever_black_24dp)
          .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
              //your deleting code
              DataStore.deleteChartEntry(mCycle.id, mEntryDate);
              dialog.dismiss();
              finish();
            }
          })
          .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              dialog.dismiss();
            }
          })
          .create().show();
    }

    if (id == android.R.id.home) {
      boolean uiDirty = false;
      if (mExistingEntry != null) {
        try {
          ChartEntry entryFromUi = getChartEntryFromUi();
          uiDirty = !mExistingEntry.equals(entryFromUi);
        } catch (Observation.InvalidObservationException ioe) {
          uiDirty = true;
        }
      }
      if (uiDirty) {
        new AlertDialog.Builder(this)
            //set message, title, and icon
            .setTitle("Discard Changes")
            .setMessage("Some of your changes have not been saved. Do you wish to discard them?")
            .setPositiveButton("Discard", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int whichButton) {
                //your deleting code
                setResult(CANCEL_RESPONSE, null);
                onBackPressed();
                dialog.dismiss();
                finish();
              }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
              }
            })
            .create().show();
      } else {
        setResult(CANCEL_RESPONSE, null);
        onBackPressed();
      }
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
}

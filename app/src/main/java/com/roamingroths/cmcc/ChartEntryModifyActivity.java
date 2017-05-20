package com.roamingroths.cmcc;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseError;
import com.roamingroths.cmcc.data.ChartEntry;
import com.roamingroths.cmcc.data.Cycle;
import com.roamingroths.cmcc.data.DataStore;
import com.roamingroths.cmcc.data.Observation;
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

  private Cycle mCycle;
  private LocalDate mEntryDate;
  private ChartEntry mExistingEntry;

  private void updateUiWithEntry(ChartEntry entry) {
    updateUiWithObservation(entry.observation);
    mPeakDaySwitch.setChecked(entry.peakDay);
    mIntercourseSwitch.setChecked(entry.intercourse);
  }

  private void updateUiWithObservation(Observation observation) {
    mObservationDescriptionTextView.setText(observation.getMultiLineDescription());
    mObservationEditText.setText(observation.toString());
    mObservationEditText.setError(null);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_observation_modify);

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
          mObservationDescriptionTextView.setText(observation.getMultiLineDescription());
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

    DataStore.getChartEntry(this, mCycle.id, entryDateStr, new DataStore.Callback<ChartEntry>() {
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
    return new ChartEntry(mEntryDate, observation, peakDay, intercourse, firstDay);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_observation_modify, menu);
    return true;
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
      Intent returnIntent = new Intent();
      try {
        returnIntent.putExtra(Cycle.class.getName(), mCycle);
        ChartEntry entry = getChartEntryFromUi();
        DataStore.putChartEntry(this, mCycle.id, entry);
        setResult(OK_RESPONSE, returnIntent);
        finish();
      } catch (Observation.InvalidObservationException ioe) {
        Toast.makeText(this, "Cannot save invalid observation", Toast.LENGTH_LONG).show();
      } catch (CryptoUtil.CryptoException e) {
        e.printStackTrace();
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

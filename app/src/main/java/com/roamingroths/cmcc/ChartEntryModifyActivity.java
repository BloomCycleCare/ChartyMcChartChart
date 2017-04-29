package com.roamingroths.cmcc;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.roamingroths.cmcc.data.ChartEntry;
import com.roamingroths.cmcc.data.Observation;

public class ChartEntryModifyActivity extends AppCompatActivity {

  private int mIndex;
  private TextInputEditText mObservationEditText;
  private TextView mObservationDescriptionTextView;

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
            Observation observation = getObservationFromView(v);
            mObservationDescriptionTextView.setText(observation.getMultiLineDescription());
            mObservationEditText.setText(observation.toString());
            mObservationEditText.setError(null);
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
      public void afterTextChanged(Editable s) {
      }
    });

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle("Monday May 1");

    Intent intentThatStartedThisActivity = getIntent();
    if (intentThatStartedThisActivity != null
        && intentThatStartedThisActivity.hasExtra(Intent.EXTRA_INDEX)) {
      mIndex = intentThatStartedThisActivity.getIntExtra(Intent.EXTRA_INDEX, -1);
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
      try {
        ChartEntry entry = new ChartEntry(getObservationFromEditText(), false, false);
        Parcel.obtain().writeParcelable(entry, 0);
      } catch (Observation.InvalidObservationException ioe) {

      } finally {
        finish();
      }
      return true;
    }

    if (id == android.R.id.home) {
      onBackPressed();
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
}

package com.roamingroths.cmcc;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.roamingroths.cmcc.data.ChartEntry;
import com.roamingroths.cmcc.data.Cycle;
import com.roamingroths.cmcc.data.DataStore;
import com.roamingroths.cmcc.utils.Callbacks;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.joda.time.LocalDate;

import java.util.Calendar;

import static com.roamingroths.cmcc.ChartEntryListActivity.RC_SIGN_IN;

public class SplashActivity extends AppCompatActivity {

  private ProgressBar mProgressBar;
  private TextView mErrorView;
  private TextView mStatusView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash);

    mProgressBar = (ProgressBar) findViewById(R.id.splash_progress);
    mErrorView = (TextView) findViewById(R.id.splash_error_tv);
    mStatusView = (TextView) findViewById(R.id.splash_status_tv);

    showProgress("Loading user account");

    // Get user
    final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
      updateStatus("Creating new user account");
      startActivityForResult(
          AuthUI.getInstance().createSignInIntentBuilder()
              .setProviders(AuthUI.GOOGLE_PROVIDER)
              .setIsSmartLockEnabled(false)
              .build(),
          RC_SIGN_IN);
    } else {
      getCurrentCycleForUser(user);
    }
  }

  private void preloadCycleData(final Cycle cycle) {
    log("Preload cycle data: start");
    ChartEntryList.builder(cycle).build().initialize(getApplicationContext(), new Callbacks.HaltingCallback<Void>() {
      @Override
      public void acceptData(Void data) {
        log("Preload cycle data: finish");
        Intent intent = new Intent(getApplicationContext(), ChartEntryListActivity.class);
        intent.putExtra(Cycle.class.getName(), cycle);
        finish();
        startActivity(intent);
      }
    });
  }

  private void promptForStartOfCurrentCycle(final FirebaseUser user) {
    log("Prompting for start of first cycle.");
    DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
        new DatePickerDialog.OnDateSetListener() {
          @Override
          public void onDateSet(
              DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
            LocalDate cycleStartDate = new LocalDate(year, monthOfYear + 1, dayOfMonth);
            log("Creating new cycle starting " + cycleStartDate.toString());
            Callbacks.Callback<Cycle> cycleCallback = new Callbacks.HaltingCallback<Cycle>() {
              @Override
              public void acceptData(final Cycle cycle) {
                preloadCycleData(cycle);
              }
            };
            Cycle previousCycle = null;
            Cycle nextCycle = null;
            LocalDate cycleEndDate = null;
            DataStore.createCycle(
                getApplicationContext(),
                user.getUid(),
                previousCycle,
                nextCycle,
                cycleStartDate,
                cycleEndDate,
                cycleCallback);
          }
        });
    datePickerDialog.setTitle("First day of current cycle");
    datePickerDialog.setMaxDate(Calendar.getInstance());
    datePickerDialog.show(getFragmentManager(), "datepickerdialog");
  }

  private void getCurrentCycleForUser(final FirebaseUser user) {
    updateStatus("Loading current cycle");
    DataStore.getCurrentCycle(user.getUid(), new Callbacks.Callback<Cycle>() {
      @Override
      public void acceptData(Cycle cycle) {
        log("Received current cycle from DB.");
        preloadCycleData(cycle);
      }

      @Override
      public void handleNotFound() {
        promptForStartOfCurrentCycle(user);
      }

      @Override
      public void handleError(DatabaseError error) {
        showError("Error fetching current cycle.");
        error.toException().printStackTrace();
      }
    });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case RC_SIGN_IN:
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
          showError("Could not create user.");
        } else {
          updateStatus("Account created successfully");
          getCurrentCycleForUser(user);
        }
        break;
      default:
        Log.w(SplashActivity.class.getName(), "Unknown request code: " + requestCode);
    }
  }

  private void showProgress(String initialStatus) {
    mProgressBar.setVisibility(View.VISIBLE);

    updateStatus(initialStatus);
    mStatusView.setVisibility(View.VISIBLE);

    mErrorView.setText("");
    mErrorView.setVisibility(View.INVISIBLE);
  }

  private void updateStatus(String status) {
    mStatusView.setText(status);
  }

  private void showError(String errorText) {
    mProgressBar.setVisibility(View.INVISIBLE);

    mStatusView.setVisibility(View.INVISIBLE);

    mErrorView.setText(errorText);
    mErrorView.setVisibility(View.VISIBLE);
  }

  private void log(String message) {
    Log.v(SplashActivity.class.getName(), message);
  }
}
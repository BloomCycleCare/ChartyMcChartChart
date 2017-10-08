package com.roamingroths.cmcc.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.Preferences;
import com.roamingroths.cmcc.data.CycleProvider;
import com.roamingroths.cmcc.data.EntryContainerList;
import com.roamingroths.cmcc.data.UserInitializationListener;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.ui.entry.list.ChartEntryListActivity;
import com.roamingroths.cmcc.utils.Callbacks;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.joda.time.LocalDate;

import java.util.Calendar;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by parkeroth on 10/8/17.
 */

public class LoadCurrentCycleFragment extends SplashFragment implements UserInitializationListener {

  private static boolean DEBUG = false;
  private static String TAG = LoadCurrentCycleFragment.class.getSimpleName();

  private Preferences mPreferences;
  private CycleProvider mCycleProvider;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPreferences = Preferences.fromShared(getApplicationContext());
    mCycleProvider = CycleProvider.forDb(FirebaseDatabase.getInstance());
  }

  @Override
  public void onUserInitialized(final FirebaseUser user) {
    mCycleProvider.getCurrentCycle(user.getUid(), new Callbacks.Callback<Cycle>() {
      @Override
      public void acceptData(Cycle cycle) {
        preloadCycleData(cycle, user);
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

  private void promptForStartOfCurrentCycle(final FirebaseUser user) {
    updateStatus("Prompting for start of first cycle.");
    updateStatus("Creating first cycle");
    DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
        new DatePickerDialog.OnDateSetListener() {
          @Override
          public void onDateSet(
              DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
            LocalDate cycleStartDate = new LocalDate(year, monthOfYear + 1, dayOfMonth);
            if (DEBUG) Log.v(TAG, "Creating new cycle starting " + cycleStartDate.toString());
            Callbacks.Callback<Cycle> cycleCallback = new Callbacks.HaltingCallback<Cycle>() {
              @Override
              public void acceptData(final Cycle cycle) {
                if (DEBUG) Log.v(TAG, "Done creating new cycle");
                preloadCycleData(cycle, user);
              }
            };
            Cycle previousCycle = null;
            Cycle nextCycle = null;
            LocalDate cycleEndDate = null;
            mCycleProvider.createCycle(
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
    datePickerDialog.show(getActivity().getFragmentManager(), "datepickerdialog");
  }

  private void preloadCycleData(final Cycle cycle, final FirebaseUser user) {
    if (DEBUG) Log.v(TAG, "Preload cycle data: start");
    updateStatus("Decrypting cycle data");
    EntryContainerList.builder(cycle, mPreferences).build().initialize(mCycleProvider, Callbacks.singleUse(new Callbacks.Callback<Void>() {
      @Override
      public void acceptData(Void data) {
        if (DEBUG) Log.v(TAG, "Preload cycle data: finish");
        Intent intent = new Intent(getApplicationContext(), ChartEntryListActivity.class);
        intent.putExtra(Cycle.class.getName(), cycle);
        getActivity().finish();
        startActivity(intent);
      }

      @Override
      public void handleNotFound() {
        showError("Could not decrypt cycle entries.");
        new AlertDialog.Builder(getActivity())
            //set message, title, and icon
            .setTitle("Delete All Cycles?")
            .setMessage("This is permanent and cannot be undone!")
            .setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
              public void onClick(final DialogInterface dialog, int whichButton) {
                mCycleProvider.dropCycles(new Callbacks.HaltingCallback<Void>() {
                  @Override
                  public void acceptData(Void data) {
                    dialog.dismiss();
                    onUserInitialized(user);
                  }
                });
              }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                showError("Please restart the app.");
                dialog.dismiss();
              }
            })
            .create().show();
      }

      @Override
      public void handleError(DatabaseError error) {
        showError(error.getMessage());
      }
    }));
  }
}

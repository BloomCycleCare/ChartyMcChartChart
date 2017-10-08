package com.roamingroths.cmcc.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.data.AppState;
import com.roamingroths.cmcc.data.CycleProvider;
import com.roamingroths.cmcc.data.UserInitializationListener;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.ui.entry.list.ChartEntryListActivity;
import com.roamingroths.cmcc.utils.Callbacks;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by parkeroth on 10/8/17.
 */

public class ImportAppStateFragment extends SplashFragment implements UserInitializationListener {

  private CycleProvider mCycleProvider;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mCycleProvider = CycleProvider.forDb(FirebaseDatabase.getInstance());
  }

  @Override
  public void onUserInitialized(final FirebaseUser user) {
    mCycleProvider.getCurrentCycle(user.getUid(), new Callbacks.Callback<Cycle>() {
      @Override
      public void acceptData(Cycle cycle) {
        updateStatus("Received current cycle from DB.");
        confirmImport(new Callbacks.SwitchingCallback() {
          @Override
          public void positive() {
            mCycleProvider.dropCycles(new Callbacks.ErrorForwardingCallback<Void>(this) {
              @Override
              public void acceptData(Void data) {
                importDataFromIntent(getActivity().getIntent(), user.getUid());
              }
            });
          }

          @Override
          public void negative() {
            getActivity().finish();
          }
        });
      }

      @Override
      public void handleNotFound() {
        importDataFromIntent(getActivity().getIntent(), user.getUid());
      }

      @Override
      public void handleError(DatabaseError error) {
        showError("Error fetching current cycle.");
        error.toException().printStackTrace();
      }
    });
  }

  private void importDataFromIntent(Intent intent, String userId) {
    Uri uri = intent.getData();
    Log.v("SplashActivity", "Reading data from " + uri.getPath());
    try {
      InputStream in = getActivity().getContentResolver().openInputStream(uri);
      AppState.parseAndPushToDB(in, userId, mCycleProvider, new Callbacks.HaltingCallback<Cycle>() {
        @Override
        public void acceptData(Cycle cycle) {
          Intent intent = new Intent(getActivity(), ChartEntryListActivity.class);
          intent.putExtra(Cycle.class.getName(), cycle);
          getActivity().finish();
          startActivity(intent);
        }
      });
    } catch (FileNotFoundException e) {
      showError("File " + uri.getPath() + " does not exist");
      return;
    }
  }

  private void confirmImport(final Callbacks.Callback<Boolean> callback) {
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        new AlertDialog.Builder(getActivity())
            //set message, title, and icon
            .setTitle("Import data from file?")
            .setMessage("This will wipe all existing data load the data from the file. This is permanent and cannot be undone!")
            .setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
              public void onClick(final DialogInterface dialog, int whichButton) {
                dialog.dismiss();
                callback.acceptData(true);
              }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                callback.acceptData(false);
              }
            })
            .create().show();
      }
    });
  }

}

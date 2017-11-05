package com.roamingroths.cmcc.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.RxCryptoUtil;
import com.roamingroths.cmcc.data.AppState;
import com.roamingroths.cmcc.data.CycleProvider;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.ui.entry.list.ChartEntryListActivity;
import com.roamingroths.cmcc.utils.Callbacks;

import java.io.FileNotFoundException;
import java.io.InputStream;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.SingleSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

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
  public void onUserInitialized(final FirebaseUser user, RxCryptoUtil cryptoUtil) {
    mCycleProvider.getCurrentCycle(user.getUid())
        .isEmpty()
        .flatMap(new Function<Boolean, SingleSource<Boolean>>() {
          @Override
          public SingleSource<Boolean> apply(@NonNull Boolean isEmpty) throws Exception {
            if (!isEmpty) {
              return confirmImport();
            }
            return Single.just(true);
          }
        })
        .subscribe(new Consumer<Boolean>() {
          @Override
          public void accept(@NonNull Boolean shouldImport) throws Exception {
            importDataFromIntent(getActivity().getIntent(), user.getUid());
          }
        });
  }

  private void importDataFromIntent(Intent intent, String userId) {
    Uri uri = intent.getData();
    Log.v("UserInitActivity", "Reading data from " + uri.getPath());
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

  private Single<Boolean> confirmImport() {
    return Single.create(new SingleOnSubscribe<Boolean>() {
      @Override
      public void subscribe(final @NonNull SingleEmitter<Boolean> e) throws Exception {
        new AlertDialog.Builder(getActivity())
            //set message, title, and icon
            .setTitle("Import data from file?")
            .setMessage("This will wipe all existing data load the data from the file. This is permanent and cannot be undone!")
            .setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
              public void onClick(final DialogInterface dialog, int whichButton) {
                dialog.dismiss();
                e.onSuccess(true);
              }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                e.onSuccess(false);
              }
            })
            .create().show();
      }
    });
  }
}

package com.roamingroths.cmcc.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.data.AppState;
import com.roamingroths.cmcc.data.CycleProvider;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.ui.entry.list.ChartEntryListActivity;

import java.io.InputStream;
import java.util.concurrent.Callable;

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
  public void onUserInitialized(final FirebaseUser user, CryptoUtil cryptoUtil) {
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
            importDataFromIntent(getActivity().getIntent(), user);
          }
        });
  }

  private void importDataFromIntent(Intent intent, final FirebaseUser user) {
    final Uri uri = intent.getData();
    Log.v("UserInitActivity", "Reading data from " + uri.getPath());
    Callable<InputStream> openFile = new Callable<InputStream>() {
      @Override
      public InputStream call() throws Exception {
        return getActivity().getContentResolver().openInputStream(uri);
      }
    };
    Single.fromCallable(openFile)
        .flatMap(new Function<InputStream, SingleSource<Cycle>>() {
          @Override
          public SingleSource<Cycle> apply(InputStream is) throws Exception {
            return AppState.parseAndPushToDB(is, user, mCycleProvider);
          }
        })
        .subscribe(new Consumer<Cycle>() {
          @Override
          public void accept(Cycle cycle) throws Exception {
            Intent intent = new Intent(getActivity(), ChartEntryListActivity.class);
            intent.putExtra(Cycle.class.getName(), cycle);
            getActivity().finish();
            startActivity(intent);
          }
        }, new Consumer<Throwable>() {
          @Override
          public void accept(Throwable t) throws Exception {
            showError("Error importing data");
            Log.e(ImportAppStateFragment.class.getSimpleName(), "Error importing data", t);
          }
        });
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

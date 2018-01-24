package com.roamingroths.cmcc.ui.init;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.logic.chart.Cycle;
import com.roamingroths.cmcc.providers.CycleProvider;
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
    mCycleProvider = MyApplication.getProviders().forCycle();
  }

  @Override
  public void onUserInitialized(final FirebaseUser user) {
    mCycleProvider.getCurrentCycle(user)
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
    MyApplication.getProviders().forAppState()
        .parseAndPushToRemote(openFile)
        .andThen(mCycleProvider.getCurrentCycle(user))
        .subscribe(new Consumer<Cycle>() {
          @Override
          public void accept(Cycle cycle) throws Exception {
            Intent intent = new Intent(getActivity(), ChartEntryListActivity.class);
            intent.putExtra(Cycle.class.getName(), cycle);
            getActivity().finish();
            startActivity(intent);
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

package com.bloomcyclecare.cmcc.ui.init;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.data.backup.AppStateImporter;
import com.bloomcyclecare.cmcc.data.backup.AppStateParser;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.repos.cycle.RWCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.entry.RWChartEntryRepo;
import com.bloomcyclecare.cmcc.ui.entry.list.ChartEntryListActivity;
import com.google.firebase.auth.FirebaseUser;

import org.parceler.Parcels;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by parkeroth on 10/8/17.
 */

public class ImportAppStateFragment extends SplashFragment implements UserInitializationListener {

  private RWCycleRepo mCycleRepo;
  private RWChartEntryRepo mEntryRepo;
  private CompositeDisposable mDisposables = new CompositeDisposable();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    MyApplication myApp = MyApplication.cast(getActivity().getApplication());
    mCycleRepo = myApp.cycleRepo();
    mEntryRepo = myApp.entryRepo();
  }

  @Override
  public void onUserInitialized(final FirebaseUser user) {
    mDisposables.add(mCycleRepo.getCurrentCycle()
        .isEmpty()
        .observeOn(AndroidSchedulers.mainThread())
        .flatMap(isEmpty -> {
          if (!isEmpty) {
            return confirmImport();
          }
          return Single.just(true);
        })
        .observeOn(Schedulers.computation())
        .flatMap(shouldImport -> {
          if (!shouldImport) {
            showProgress("Not importing data.");
            return mCycleRepo.getCurrentCycle().toSingle();
          }
          showProgress("Importing data.");
          AppStateImporter importer = new AppStateImporter(MyApplication.cast(getActivity().getApplication()));
          return AppStateParser.parse(() -> getActivity()
              .getContentResolver()
              .openInputStream(getActivity().getIntent().getData()))
              .doOnSuccess(appState -> showProgress("Successfully parsed data."))
              .flatMapCompletable(appState -> Completable.mergeArray(
                  mCycleRepo.deleteAll(),
                  mEntryRepo.deleteAll())
                  .doOnComplete(() -> showProgress("Done clearing old data."))
                  .andThen(importer.importAppState(appState))
                  .doOnComplete(() -> showProgress("Done importing data.")))
              .andThen(mCycleRepo
                  .getCurrentCycle()
                  .toSingle());
        })
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(cycle -> {
          showProgress("Staring app");
          Intent intent = new Intent(getActivity(), ChartEntryListActivity.class);
          intent.putExtra(Cycle.class.getName(), Parcels.wrap(cycle));
          getActivity().finish();
          startActivity(intent);
        }, throwable -> {
          showError("Error importing data");
          Timber.e(throwable);
        }));
  }

  @Override
  public void onDestroy() {
    mDisposables.clear();
    super.onDestroy();
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

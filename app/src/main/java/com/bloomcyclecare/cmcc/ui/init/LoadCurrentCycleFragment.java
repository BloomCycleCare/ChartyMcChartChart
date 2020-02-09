package com.bloomcyclecare.cmcc.ui.init;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.data.backup.AppStateImporter;
import com.bloomcyclecare.cmcc.data.backup.AppStateParser;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.repos.CycleRepo;
import com.bloomcyclecare.cmcc.ui.entry.list.ChartEntryListActivity;
import com.google.api.services.drive.model.File;
import com.google.firebase.auth.FirebaseUser;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.joda.time.LocalDate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Calendar;

import io.reactivex.Maybe;
import io.reactivex.MaybeTransformer;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by parkeroth on 10/8/17.
 */

public class LoadCurrentCycleFragment extends SplashFragment implements UserInitializationListener {

  private Activity activity;
  private CompositeDisposable mDisposables = new CompositeDisposable();
  private CycleRepo mCycleRepo;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mCycleRepo = MyApplication.cast(getActivity().getApplication()).cycleRepo();
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    activity = getActivity();
  }

  @Override
  public void onDestroy() {
    mDisposables.clear();
    super.onDestroy();
  }

  @Override
  public void onUserInitialized(final FirebaseUser user) {
    updateStatus("Loading your data");
    Timber.v("Getting current cycleToShow");
    mDisposables.add(mCycleRepo.getCurrentCycle()
        .compose(tryUseLatestAsCurrent())
        .observeOn(AndroidSchedulers.mainThread())
        .compose(tryRestoreFromDrive())
        .compose(initFirstCycle())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.computation())
        .subscribe(cycle -> {
          Intent intent = new Intent(getContext(), ChartEntryListActivity.class);
          startActivity(intent);
        }, throwable -> {
          Timber.e(throwable);
          showError("Could not find or create a cycle!");
        }));
  }

  private MaybeTransformer<Cycle, Cycle> tryUseLatestAsCurrent() {
    return upstream -> upstream
        .switchIfEmpty(mCycleRepo.getLatestCycle()
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap(latestCycle -> promptUseLatestAsCurrent()
                .observeOn(Schedulers.computation())
                .flatMapMaybe(useCurrent -> {
                  if (!useCurrent) {
                    return Maybe.empty();
                  }
                  Cycle copyOfLatest = new Cycle(latestCycle);
                  copyOfLatest.endDate = null;
                  return mCycleRepo.insertOrUpdate(copyOfLatest).andThen(Maybe.just(copyOfLatest));
                })));
  }

  private MaybeTransformer<Cycle, Cycle> tryRestoreFromDrive() {
    return upstream -> upstream.switchIfEmpty(
        MyApplication.getInstance().preferenceRepo().summaries().firstOrError()
            .flatMapMaybe(summary -> {
              if (!summary.backupEnabled()) {
                Timber.i("Not restoring from Drive, backup disabled");
                return Maybe.empty();
              }
              return MyApplication.getInstance().driveService()
                  .flatMapMaybe(driveHelper -> {
                    if (!driveHelper.isPresent()) {
                      Timber.i("Not restoring from Drive, service not available");
                      return Maybe.empty();
                    }
                    return Maybe.just(driveHelper.get());
                  })
                  .flatMap(driveService -> driveService.getFolder("My Charts")
                      .flatMap(folder -> driveService.getFilesInFolder(folder, "backup.chart"))
                      .observeOn(AndroidSchedulers.mainThread())
                      .flatMap(files -> {
                        if (files.size() > 1) {
                          Timber.w("Found several files! Bailing out of restore flow.");
                          return Maybe.empty();
                        }
                        File backupFile = files.get(0);
                        Timber.d("Prompting for restore");
                        return promptRestoreFromDrive(backupFile).observeOn(Schedulers.io()).flatMapMaybe(doRestore -> {
                          if (!doRestore) {
                            return Maybe.empty();
                          }
                          Timber.d("Reading backup files from Drive");
                          ByteArrayOutputStream out = new ByteArrayOutputStream();
                          return driveService.downloadFile(files.get(0), out)
                              .map(outputStream -> out.toByteArray())
                              .toMaybe();
                        });
                      }))
                  .observeOn(Schedulers.computation())
                  .flatMap(bytes -> {
                    Timber.d("Parsing backup file");
                    ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                    return AppStateParser.parse(() -> in).toMaybe();
                  })
                  .flatMap(appState -> {
                    Timber.d("Importing app state");
                    AppStateImporter importer = new AppStateImporter(MyApplication.getInstance());
                    return importer.importAppState(appState).andThen(mCycleRepo.getCurrentCycle());
                  });
            }).doOnSubscribe(d -> Timber.d("Trying restore from Drive")));
  }

  private MaybeTransformer<Cycle, Cycle> initFirstCycle() {
    return upstream -> upstream
        .switchIfEmpty(promptForStart()
            .observeOn(Schedulers.computation())
            .flatMap(startDate -> {
              Cycle cycle = new Cycle("foo", LocalDate.now(), null);
              return mCycleRepo.insertOrUpdate(cycle).andThen(Single.just(cycle));
            }))
        .toMaybe();
  }

  private Single<Boolean> promptUseLatestAsCurrent() {
    return Single.create(e -> {
      new AlertDialog.Builder(getActivity())
          //set message, title, and icon
          .setTitle("Use latest cycle?")
          .setMessage("Would you like to use the latest cycle as the current? Probably recovering from an error...")
          .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
              e.onSuccess(true);
              dialog.dismiss();
            }
          })
          .setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              e.onSuccess(false);
              dialog.dismiss();
            }
          })
          .create().show();
    });
  }

  private Single<Boolean> promptRestoreFromDrive(File file) {
    return Single.create(e -> {
      new AlertDialog.Builder(getActivity())
          //set message, title, and icon
          .setTitle("Restore from Drive?")
          .setMessage(String.format("Would you like to restore your data from Google Drive? Last backup was taken on %s", file.getModifiedTime()))
          .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
              e.onSuccess(true);
              dialog.dismiss();
            }
          })
          .setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              e.onSuccess(false);
              dialog.dismiss();
            }
          })
          .create().show();
    });
  }

  private Single<LocalDate> promptForStart() {
    return Single.create(e -> {
      updateStatus("Prompting for start of first cycleToShow.");
      updateStatus("Creating first cycleToShow");
      DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
          (view, year, monthOfYear, dayOfMonth) -> e.onSuccess(new LocalDate(year, monthOfYear + 1, dayOfMonth)));
      datePickerDialog.setTitle("First day of current cycleToShow");
      datePickerDialog.setMaxDate(Calendar.getInstance());
      datePickerDialog.show(activity.getFragmentManager(), "datepickerdialog");
    });
  }
}

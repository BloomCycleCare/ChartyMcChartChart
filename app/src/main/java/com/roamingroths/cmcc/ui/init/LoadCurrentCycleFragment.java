package com.roamingroths.cmcc.ui.init;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.providers.CycleProvider;
import com.roamingroths.cmcc.ui.entry.list.ChartEntryListActivity;
import com.roamingroths.cmcc.utils.UpdateHandle;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.joda.time.LocalDate;

import java.util.Calendar;

import io.reactivex.CompletableSource;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;

/**
 * Created by parkeroth on 10/8/17.
 */

public class LoadCurrentCycleFragment extends SplashFragment implements UserInitializationListener {

  private static boolean DEBUG = false;
  private static String TAG = LoadCurrentCycleFragment.class.getSimpleName();

  private CompositeDisposable mDisposables = new CompositeDisposable();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onDestroy() {
    mDisposables.clear();
    super.onDestroy();
  }

  @Override
  public void onUserInitialized(final FirebaseUser user) {
    updateStatus("Loading your data");
    if (DEBUG) Log.v(TAG, "Getting current cycleToShow");
    mDisposables.add(
        MyApplication.cycleProvider()
            .flatMap(cycleProvider -> cycleProvider.getOrCreateCurrentCycle(user, promptForStart()))
            .flatMap(cycle -> MyApplication.chartEntryProvider().flatMap(
                chartEntryProvider -> chartEntryProvider.maybeAddNewEntriesDeferred(cycle)
                    .flatMapCompletable(UpdateHandle.run())
                    .andThen(Single.just(cycle))))
        .subscribe(cycle -> {
          Intent intent = new Intent(getContext(), ChartEntryListActivity.class);
          startActivity(intent);
        }, throwable -> {
          showError("Could not decrypt cycleToShow entries.");
          new AlertDialog.Builder(getActivity())
              //set message, title, and icon
              .setTitle("Delete All Cycles?")
              .setMessage("This is permanent and cannot be undone!")
              .setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog, int whichButton) {
                  MyApplication.cycleProvider().flatMapCompletable(new Function<CycleProvider, CompletableSource>() {
                    @Override
                    public CompletableSource apply(CycleProvider cycleProvider) throws Exception {
                      return cycleProvider.dropCycles(user).flatMapCompletable(UpdateHandle.run());
                    }
                  }).subscribe(new Action() {
                    @Override
                    public void run() throws Exception {
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
        }));
  }

  Single<LocalDate> promptForStart() {
    return Single.create(new SingleOnSubscribe<LocalDate>() {
      @Override
      public void subscribe(final @NonNull SingleEmitter<LocalDate> e) throws Exception {
        updateStatus("Prompting for start of first cycleToShow.");
        updateStatus("Creating first cycleToShow");
        DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
            new DatePickerDialog.OnDateSetListener() {
              @Override
              public void onDateSet(
                  DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                e.onSuccess(new LocalDate(year, monthOfYear + 1, dayOfMonth));
              }
            });
        datePickerDialog.setTitle("First day of current cycleToShow");
        datePickerDialog.setMaxDate(Calendar.getInstance());
        datePickerDialog.show(getActivity().getFragmentManager(), "datepickerdialog");
      }
    });
  }
}

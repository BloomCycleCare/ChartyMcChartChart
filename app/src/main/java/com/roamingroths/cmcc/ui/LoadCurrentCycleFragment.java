package com.roamingroths.cmcc.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.common.collect.Lists;
import com.google.firebase.auth.FirebaseUser;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.CycleProvider;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.ui.entry.list.ChartEntryListActivity;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.joda.time.LocalDate;

import java.util.Calendar;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

import static com.facebook.FacebookSdk.getApplicationContext;

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
    if (DEBUG) Log.v(TAG, "Getting current cycle");
    final CycleProvider cycleProvider = MyApplication.getProviders().forCycle();
    mDisposables.add(cycleProvider.getOrCreateCurrentCycle(user.getUid(), promptForStart())
        .subscribe(new Consumer<Cycle>() {
          @Override
          public void accept(@NonNull Cycle cycle) throws Exception {
            preloadCycleData(cycleProvider, cycle, user);
          }
        }, new Consumer<Throwable>() {
          @Override
          public void accept(@NonNull Throwable throwable) throws Exception {
            Log.e(TAG, "Error loading current cycle.", throwable);
          }
        }));
  }

  Single<LocalDate> promptForStart() {
    return Single.create(new SingleOnSubscribe<LocalDate>() {
      @Override
      public void subscribe(final @NonNull SingleEmitter<LocalDate> e) throws Exception {
        updateStatus("Prompting for start of first cycle.");
        updateStatus("Creating first cycle");
        DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
            new DatePickerDialog.OnDateSetListener() {
              @Override
              public void onDateSet(
                  DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                e.onSuccess(new LocalDate(year, monthOfYear + 1, dayOfMonth));
              }
            });
        datePickerDialog.setTitle("First day of current cycle");
        datePickerDialog.setMaxDate(Calendar.getInstance());
        datePickerDialog.show(getActivity().getFragmentManager(), "datepickerdialog");
      }
    });
  }

  private void preloadCycleData(final CycleProvider cycleProvider, final Cycle cycle, final FirebaseUser user) {
    if (DEBUG) Log.v(TAG, "Preload cycle data: start");
    updateStatus("Decrypting cycle data");
    cycleProvider.maybeCreateNewEntries(cycle)
        .andThen(cycleProvider.getEntries(cycle))
        .toList()
        .subscribe(new Consumer<List<ChartEntry>>() {
          @Override
          public void accept(List<ChartEntry> chartEntries) throws Exception {
            if (DEBUG) Log.v(TAG, "Preload cycle data: finish");
            Intent intent = new Intent(getApplicationContext(), ChartEntryListActivity.class);
            intent.putExtra(Cycle.class.getName(), cycle);
            intent.putParcelableArrayListExtra(ChartEntry.class.getName(), Lists.newArrayList(chartEntries));
            // TODO fix "not attached to activity"
            startActivity(intent);
          }
        }, new Consumer<Throwable>() {
          @Override
          public void accept(Throwable throwable) throws Exception {
            showError("Could not decrypt cycle entries.");
            new AlertDialog.Builder(getActivity())
                //set message, title, and icon
                .setTitle("Delete All Cycles?")
                .setMessage("This is permanent and cannot be undone!")
                .setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
                  public void onClick(final DialogInterface dialog, int whichButton) {
                    cycleProvider.dropCycles()
                        .subscribe(new Action() {
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
          }
        });
  }
}

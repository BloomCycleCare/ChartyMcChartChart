package com.roamingroths.cmcc.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.common.collect.Lists;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.Preferences;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.data.CycleProvider;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.EntryContainer;
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

  private Preferences mPreferences;
  private CycleProvider mCycleProvider;
  private CompositeDisposable mDisposables;
  private CryptoUtil mCryptoUtil;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPreferences = Preferences.fromShared(getApplicationContext());
  }

  @Override
  public void onUserInitialized(final FirebaseUser user, CryptoUtil cryptoUtil) {
    if (DEBUG) Log.v(TAG, "Getting current cycle");
    mCycleProvider = CycleProvider.forDb(FirebaseDatabase.getInstance(), cryptoUtil);
    mCryptoUtil = cryptoUtil;
    mDisposables.add(mCycleProvider.getOrCreateCurrentCycle(user.getUid(), promptForStart(user))
        .subscribe(new Consumer<Cycle>() {
          @Override
          public void accept(@NonNull Cycle cycle) throws Exception {
            preloadCycleData(cycle, user);
          }
        }, new Consumer<Throwable>() {
          @Override
          public void accept(@NonNull Throwable throwable) throws Exception {
            Log.e(TAG, "Error loading current cycle.", throwable);
          }
        }));
  }

  Single<LocalDate> promptForStart(final FirebaseUser user) {
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

  private void preloadCycleData(final Cycle cycle, final FirebaseUser user) {
    if (DEBUG) Log.v(TAG, "Preload cycle data: start");
    updateStatus("Decrypting cycle data");
    mCycleProvider.maybeCreateNewEntries(cycle)
        .andThen(mCycleProvider.getEntryContainers(cycle))
        .toList()
        .subscribe(new Consumer<List<EntryContainer>>() {
          @Override
          public void accept(List<EntryContainer> entryContainers) throws Exception {
            if (DEBUG) Log.v(TAG, "Preload cycle data: finish");
            Intent intent = new Intent(getApplicationContext(), ChartEntryListActivity.class);
            intent.putExtra(Cycle.class.getName(), cycle);
            intent.putParcelableArrayListExtra(EntryContainer.class.getName(), Lists.newArrayList(entryContainers));
            getActivity().finish();
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
                    mCycleProvider.dropCycles()
                        .subscribe(new Action() {
                          @Override
                          public void run() throws Exception {
                            dialog.dismiss();
                            onUserInitialized(user, mCryptoUtil);
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

package com.bloomcyclecare.cmcc.ui.init;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.apps.charting.ViewMode;
import com.bloomcyclecare.cmcc.backup.AppStateImporter;
import com.bloomcyclecare.cmcc.backup.AppStateParser;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.repos.cycle.RWCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.instructions.ROInstructionsRepo;
import com.bloomcyclecare.cmcc.data.repos.pregnancy.RWPregnancyRepo;
import com.bloomcyclecare.cmcc.ui.init.ftue.StepperFragment;
import com.bloomcyclecare.cmcc.ui.main.MainActivity;
import com.google.api.services.drive.model.File;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Queues;
import com.google.firebase.auth.FirebaseUser;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.joda.time.LocalDate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.Map;
import java.util.Queue;

import io.reactivex.Completable;
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
  private RWCycleRepo mCycleRepo;
  private RWPregnancyRepo mPregnancyRepo;
  private ROInstructionsRepo mInstructionsRepo;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ChartingApp myApp = ChartingApp.cast(requireActivity().getApplication());
    mCycleRepo = myApp.cycleRepo(ViewMode.CHARTING);
    mPregnancyRepo = myApp.pregnancyRepo(ViewMode.CHARTING);
    mInstructionsRepo = myApp.instructionsRepo(ViewMode.CHARTING);
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    activity = getActivity();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);
    // TODO: Drop https://github.com/parkeroth/ChartyMcChartChart/issues/17
    onUserInitialized(null);
    return view;
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
    getChildFragmentManager().beginTransaction().add(R.layout.fragment_stepper, new StepperFragment()).commit();

    mDisposables.add(mCycleRepo.getCurrentCycle()
        .compose(tryUseLatestAsCurrent())
        .observeOn(AndroidSchedulers.mainThread())
        .compose(tryRestoreFromDrive())
        .observeOn(AndroidSchedulers.mainThread())
        .switchIfEmpty(runInitFlow())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.computation())
        .subscribe(cycle -> {
          Intent intent = new Intent(getContext(), MainActivity.class);
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
        ChartingApp.getInstance().preferenceRepo().summaries().firstOrError()
            .flatMapMaybe(summary -> {
              if (!summary.backupEnabled()) {
                Timber.i("Not restoring from Drive, backup disabled");
                return Maybe.empty();
              }
              return ChartingApp.getInstance().driveService()
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
                    AppStateImporter importer = new AppStateImporter(ChartingApp.getInstance());
                    return importer.importAppState(appState).andThen(mCycleRepo.getCurrentCycle());
                  });
            }).doOnSubscribe(d -> Timber.d("Trying restore from Drive")));
  }

  @AutoValue
  static abstract class InitPrompt {
    abstract String title();
    abstract String subtitle();
    abstract Single<Cycle> onPositive();

    public static InitPrompt create(String title, String subtitle, Single<Cycle> onPositive) {
      return new AutoValue_LoadCurrentCycleFragment_InitPrompt(title, subtitle, onPositive);
    }

    Maybe<Cycle> doPrompt(Context context) {
      return Single.<Boolean>create(emitter -> {
        Dialog d = new AlertDialog.Builder(context)
            .setTitle(title())
            .setMessage(subtitle())
            .setPositiveButton("Yes", (dialog, whichButton) -> {
              emitter.onSuccess(true);
              dialog.dismiss();
            })
            .setNegativeButton("No", (dialog, which) -> {
              emitter.onSuccess(false);
              dialog.dismiss();
            })
            .setOnCancelListener(dialog -> {
              emitter.onSuccess(false);
              dialog.dismiss();
            })
            .setOnDismissListener(dialog -> {
              emitter.onSuccess(false);
              dialog.dismiss();
            })
            .create();
        d.setCancelable(false);
        d.setCanceledOnTouchOutside(false);
        d.show();
      })
          .flatMapMaybe(positive -> {
            if (positive) {
              return onPositive().toMaybe();
            }
            return Maybe.empty();
          });
    }
  }

  private Single<Cycle> runInitFlow() {
    Queue<InitPrompt> prompts = Queues.newArrayDeque();
    prompts.add(InitPrompt.create(
        "Currently Pregnant?",
        "Are you currently pregnant?",
        Single.defer(this::initPregnancy)));
    prompts.add(InitPrompt.create(
        "Postpartum?",
        "Are you postpartum before your period returns?",
        Single.defer(this::initPostpartum)));
    return Single.just(true)
        .flatMapCompletable(agree -> {
          if (!agree) {
            requireActivity().finish();
          }
          return promptStartInit();
        })
        .andThen(runInitFlow(prompts))
        .switchIfEmpty(Single.defer(this::initFirstCycle))
        .doOnSubscribe(d -> Timber.d("Running init flow"))
        .doOnSuccess(cycle -> Timber.d("Initialized first cycle starting %s", cycle.startDateStr));
  }

  private Completable promptStartInit() {
    return Completable.create(emitter -> {
      Dialog dialog = new AlertDialog.Builder(requireContext())
          .setTitle("App Initialization")
          .setMessage("Thank you for agreeing to our terms of use. The next series of dialogs will guide you through the process of initializing your first cycle.")
          .setPositiveButton("Continue", (d, w) -> {
            emitter.onComplete();
            d.dismiss();
          }).create();
      dialog.setCancelable(false);
      dialog.setCanceledOnTouchOutside(false);
      dialog.show();
    });
  }



  private Maybe<Cycle> runInitFlow(Queue<InitPrompt> remainingPrompts) {
    if (remainingPrompts.isEmpty()) {
      return Maybe.empty();
    }
    return remainingPrompts.remove()
        .doPrompt(requireContext())
        .switchIfEmpty(Maybe.defer(() -> runInitFlow(remainingPrompts)));
  }

  private Single<Cycle> initPregnancy() {
    return promptForPregnancyTestDate()
        .observeOn(Schedulers.computation())
        .flatMap(pregnancyTestDate -> {
          Cycle firstCycle = new Cycle("first", pregnancyTestDate, null, null);
          return mCycleRepo.insertOrUpdate(firstCycle)
              .andThen(mPregnancyRepo.startPregnancy(pregnancyTestDate))
              .andThen(mCycleRepo.getCurrentCycle().toSingle());
        });
  }

  private Single<Cycle> initPostpartum() {
    return promptForDeliveryDate()
        .observeOn(Schedulers.computation())
        .flatMap(deliveryDate -> {
          Cycle firstCycle = new Cycle("first", deliveryDate.plusDays(1), null, null);
          return mCycleRepo.insertOrUpdate(firstCycle).andThen(Single.just(firstCycle));
        });
  }

  private Single<Cycle> initFirstCycle() {
    Timber.d("Prompting for first cycle");
    return promptForStart()
        .observeOn(Schedulers.computation())
        .flatMap(startDate -> {
          Cycle cycle = new Cycle("first", startDate, null, null);
          Timber.d("Initializing cycle starting %s", cycle.startDateStr);
          return mCycleRepo.insertOrUpdate(cycle).andThen(Single.just(cycle));
        });
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
    StringBuilder content = new StringBuilder();
    content.append("Would you like to restore your data from Google Drive?<br/><br/>");
    content.append(String.format("<b>Last backup was taken on:</b> %s", file.getModifiedTime()));
    if (file.getProperties() != null) {
      content.append("<br/><br/>");
      content.append("<b>Properties:</b>");
      for (Map.Entry<String, String> entry : file.getProperties().entrySet()) {
        content.append(String.format("<br/>%s: %s", entry.getKey(), entry.getValue()));
      }
    }
    return Single.create(e -> {
      new AlertDialog.Builder(getActivity())
          //set message, title, and icon
          .setTitle("Restore from Drive?")
          .setMessage(Html.fromHtml(content.toString()))
          .setPositiveButton("Yes", (dialog, whichButton) -> {
            e.onSuccess(true);
            dialog.dismiss();
          })
          .setNegativeButton("No", (dialog, which) -> {
            e.onSuccess(false);
            dialog.dismiss();
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
      datePickerDialog.setTitle("First day of last period");
      datePickerDialog.setMaxDate(Calendar.getInstance());
      datePickerDialog.setCancelable(false);
      datePickerDialog.show(activity.getFragmentManager(), "datepickerdialog");
    });
  }

  private Single<LocalDate> promptForPregnancyTestDate() {
    return Single.create(e -> {
      DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
          (view, year, monthOfYear, dayOfMonth) -> e.onSuccess(new LocalDate(year, monthOfYear + 1, dayOfMonth)));
      datePickerDialog.setTitle("Date of positive pregnancy test");
      datePickerDialog.setMaxDate(Calendar.getInstance());
      datePickerDialog.show(activity.getFragmentManager(), "datepickerdialog");
    });
  }

  private Single<LocalDate> promptForDeliveryDate() {
    return Single.create(e -> {
      DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
          (view, year, monthOfYear, dayOfMonth) -> e.onSuccess(new LocalDate(year, monthOfYear + 1, dayOfMonth)));
      datePickerDialog.setTitle("Date of delivery");
      datePickerDialog.setMaxDate(Calendar.getInstance());
      datePickerDialog.show(activity.getFragmentManager(), "datepickerdialog");
    });
  }
}

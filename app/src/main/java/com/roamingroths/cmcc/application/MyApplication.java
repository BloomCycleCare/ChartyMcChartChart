package com.roamingroths.cmcc.application;

import android.app.Application;
import android.content.Intent;

import com.google.api.client.http.FileContent;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Range;
import com.google.common.io.Files;
import com.google.firebase.FirebaseApp;
import com.roamingroths.cmcc.BuildConfig;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.backup.AppStateExporter;
import com.roamingroths.cmcc.data.db.AppDatabase;
import com.roamingroths.cmcc.data.drive.DriveServiceHelper;
import com.roamingroths.cmcc.data.repos.ChartEntryRepo;
import com.roamingroths.cmcc.data.repos.CycleRepo;
import com.roamingroths.cmcc.data.repos.InstructionsRepo;
import com.roamingroths.cmcc.logic.drive.SyncTrigger;
import com.roamingroths.cmcc.logic.drive.SyncWorker;
import com.roamingroths.cmcc.notifications.ChartingReceiver;
import com.roamingroths.cmcc.utils.GsonUtil;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.joda.time.LocalDate;

import java.io.File;
import java.security.Security;
import java.util.concurrent.TimeUnit;

import androidx.preference.PreferenceManager;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.SingleSubject;
import timber.log.Timber;

/**
 * Created by parkeroth on 5/15/17.
 */

public class MyApplication extends Application {

  private static ViewModelFactory mViewModelFactory;

  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private final SingleSubject<Optional<DriveServiceHelper>> mDriveSubject = SingleSubject.create();

  private AppDatabase mDB;
  private InstructionsRepo mInstructionsRepo;
  private CycleRepo mCycleRepo;
  private ChartEntryRepo mChartEntryRepo;

  public void registerDriveService(Optional<DriveServiceHelper> driveService) {
    mDriveSubject.onSuccess(driveService);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    FirebaseApp.initializeApp(this);
    //FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    Security.addProvider(new BouncyCastleProvider());
    PreferenceManager.setDefaultValues(this, R.xml.pref_settings, false);

    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree());
    }

    Migration[] migrations = new Migration[AppDatabase.MIGRATIONS.size()];
    AppDatabase.MIGRATIONS.toArray(migrations);
    mDB = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "room-db")
        .addMigrations(migrations)
        //.fallbackToDestructiveMigration()  // I'm sure this will bite me in the end...
        .build();

    //DriveServiceHelper.init(this).subscribe(mDriveSubject);

    mInstructionsRepo = new InstructionsRepo(this);
    mChartEntryRepo = new ChartEntryRepo(mDB);
    mCycleRepo = new CycleRepo(mDB);

    mViewModelFactory = new ViewModelFactory();

    Timber.i("Sending charting reminder restart intent");
    Intent chartingRestartIntent = new Intent(this, ChartingReceiver.class);
    sendBroadcast(chartingRestartIntent);

    AppStateExporter exporter = new AppStateExporter(this);
    mDisposables.add(exporter
        .export()
        .map(appState -> GsonUtil.getGsonInstance().toJson(appState))
        .map(json -> {
          File path = new File(this.getFilesDir(), "backup/");
          if (!path.exists()) {
            path.mkdir();
          }
          File file = new File(path, "cmcc_export.chart");
          Files.write(json, file, Charsets.UTF_8);
          return file;
        })
        .flatMapCompletable(file -> driveService().filter(Optional::isPresent).map(Optional::get).flatMapCompletable(driveService -> driveService
            .getOrCreateFolder("My Charts")
            .flatMap(folder -> {
              com.google.api.services.drive.model.File driveFile =
                  new com.google.api.services.drive.model.File();
              driveFile.setName("backup.chart");
              FileContent mediaContent = new FileContent("application/json", file);
              return driveService
                  .addFileToFolder(folder, driveFile, mediaContent);
            })
            .ignoreElement()))
        .subscribe());

    Flowable<SyncTrigger> triggerStream = Flowable.merge(
        cycleRepo().updateEvents().map(e -> new SyncTrigger(e.updateTime, e.dateRange)),
        entryRepo().updateEvents().flatMap(e -> cycleRepo()
            .getCycleForDate(e.updateTarget).toSingle()
            .map(cycle -> Range.closed(cycle.startDate, Optional.fromNullable(cycle.endDate).or(LocalDate.now())))
            .map(range -> new SyncTrigger(e.updateTime, range))
            .toFlowable()),
        instructionsRepo().updateEvents().map(e -> new SyncTrigger(e.updateTime, e.dateRange)))
        .share();

    Flowable<Boolean> batchSeparator = Flowable.combineLatest(
        triggerStream,
        triggerStream.delay(30, TimeUnit.SECONDS),
        (latest, delayed) -> latest.triggerTime == delayed.triggerTime)
        .doOnNext(b -> Timber.v("Separator update: %b", b))
        .filter(v -> v);

    mDisposables.add(triggerStream
        .doOnNext(t -> Timber.v("New update event"))
        .buffer(batchSeparator)
        .doOnNext(t -> Timber.v("New update batch"))
        .map(triggers -> {
          LocalDate start = null;
          LocalDate end = null;
          for (SyncTrigger trigger : triggers) {
            if (trigger.dateRange.hasLowerBound() && (start == null || start.isAfter(trigger.dateRange.lowerEndpoint()))) {
              start = trigger.dateRange.lowerEndpoint();
            }
            if (trigger.dateRange.hasUpperBound() && (end == null || end.isBefore(trigger.dateRange.upperEndpoint()))) {
              end = trigger.dateRange.upperEndpoint();
            }
          }
          if (start == null) {
            throw new IllegalArgumentException();
          }
          return Range.closed(start, Optional.fromNullable(end).or(LocalDate.now()));
        })
        .map(dateRange -> new OneTimeWorkRequest.Builder(SyncWorker.class)
            .setInputData(SyncWorker.createInputData(dateRange))
            .build())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(request -> {
          Timber.d("Received work request for sync");
          WorkManager.getInstance(getApplicationContext()).enqueue(request);
        }, Timber::e));
  }

  public Single<Optional<DriveServiceHelper>> driveService() {
    return mDriveSubject;
  }

  @Deprecated
  public AppDatabase db() {
    return mDB;
  }

  public InstructionsRepo instructionsRepo() {
    return mInstructionsRepo;
  }

  public CycleRepo cycleRepo() {
    return mCycleRepo;
  }

  public ChartEntryRepo entryRepo() {
    return mChartEntryRepo;
  }

  public static MyApplication cast(Application app) {
    return (MyApplication) app;
  }

  public static ViewModelFactory viewModelFactory() {
    return mViewModelFactory;
  }
}

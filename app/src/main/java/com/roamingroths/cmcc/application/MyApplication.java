package com.roamingroths.cmcc.application;

import android.app.Application;
import android.content.Intent;

import com.google.common.base.Optional;
import com.google.common.collect.Range;
import com.google.firebase.FirebaseApp;
import com.roamingroths.cmcc.BuildConfig;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.db.AppDatabase;
import com.roamingroths.cmcc.data.drive.DriveServiceHelper;
import com.roamingroths.cmcc.data.repos.ChartEntryRepo;
import com.roamingroths.cmcc.data.repos.CycleRepo;
import com.roamingroths.cmcc.data.repos.InstructionsRepo;
import com.roamingroths.cmcc.logic.PreferenceRepo;
import com.roamingroths.cmcc.logic.drive.BackupWorker;
import com.roamingroths.cmcc.logic.drive.PublishWorker;
import com.roamingroths.cmcc.logic.drive.UpdateTrigger;
import com.roamingroths.cmcc.notifications.ChartingReceiver;
import com.roamingroths.cmcc.utils.RxUtil;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.joda.time.LocalDate;

import java.security.Security;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.preference.PreferenceManager;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.SingleSubject;
import timber.log.Timber;

/**
 * Created by parkeroth on 5/15/17.
 */

public class MyApplication extends Application {

  private static MyApplication INSTANCE;

  private static ViewModelFactory mViewModelFactory;

  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private final SingleSubject<Optional<DriveServiceHelper>> mDriveSubject = SingleSubject.create();

  private AppDatabase mDB;
  private InstructionsRepo mInstructionsRepo;
  private CycleRepo mCycleRepo;
  private ChartEntryRepo mChartEntryRepo;
  private PreferenceRepo mPreferenceRepo;

  public void registerDriveService(Optional<DriveServiceHelper> driveService) {
    mDriveSubject.onSuccess(driveService);
  }

  public static MyApplication getInstance() {
    return INSTANCE;
  };

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

    mInstructionsRepo = new InstructionsRepo(this);
    mChartEntryRepo = new ChartEntryRepo(mDB);
    mCycleRepo = new CycleRepo(mDB);
    mPreferenceRepo = PreferenceRepo.create(this);

    mViewModelFactory = new ViewModelFactory();

    Timber.i("Sending charting reminder restart intent");
    Intent chartingRestartIntent = new Intent(this, ChartingReceiver.class);
    sendBroadcast(chartingRestartIntent);


    Flowable<UpdateTrigger> triggerStream = Flowable.merge(
        cycleRepo().updateEvents().map(e -> new UpdateTrigger(e.updateTime, e.dateRange)),
        entryRepo().updateEvents().flatMap(e -> cycleRepo()
            .getCycleForDate(e.updateTarget).toSingle()
            .map(cycle -> Range.closed(cycle.startDate, Optional.fromNullable(cycle.endDate).or(LocalDate.now())))
            .map(range -> new UpdateTrigger(e.updateTime, range))
            .toFlowable()),
        instructionsRepo().updateEvents().map(e -> new UpdateTrigger(e.updateTime, e.dateRange)))
        .share();

    Flowable<List<UpdateTrigger>> batchedTriggers = triggerStream
        .doOnNext(t -> Timber.v("New update event"))
        .buffer(Flowable.combineLatest(
            // Watch the trigger stream
            triggerStream,
            // And a rebroadcast of the stream
            triggerStream.delay(30, TimeUnit.SECONDS),
            // Looking for when the rebroadcast == the trigger stream
            (latest, delayed) -> latest.triggerTime == delayed.triggerTime)
            .doOnNext(b -> Timber.v("Separator update: %b", b))
            // Filter for only these cases
            .filter(v -> v))
        .doOnNext(t -> Timber.v("New update batch"))
        .share();

    Flowable<Range<LocalDate>> mergedTrigger = batchedTriggers
        .map(triggers -> {
          LocalDate start = null;
          LocalDate end = null;
          for (UpdateTrigger trigger : triggers) {
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
        });

    mDisposables.add(mergedTrigger
        .compose(RxUtil.takeWhile(mPreferenceRepo.summaries(), PreferenceRepo.PreferenceSummary::backupEnabled))
        .map(dateRange -> new OneTimeWorkRequest.Builder(PublishWorker.class)
            .setInputData(PublishWorker.createInputData(dateRange))
            .build())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(request -> {
          Timber.d("Received work request for publish");
          WorkManager.getInstance(getApplicationContext()).enqueue(request);
        }, Timber::e));

    mDisposables.add(batchedTriggers
        //.startWith(ImmutableList.<UpdateTrigger>of())
        .compose(RxUtil.onceAvailable(driveService()))
        .map(trigger -> new OneTimeWorkRequest.Builder(BackupWorker.class).build())
        .compose(RxUtil.takeWhile(mPreferenceRepo.summaries(), PreferenceRepo.PreferenceSummary::backupEnabled))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(request -> {
          Timber.d("Received work request for backup");
          WorkManager.getInstance(getApplicationContext()).enqueue(request);
        }, Timber::e));

    mDisposables.add(mDriveSubject.subscribe(
        s -> Timber.d("DriveServiceHelper initialized."),
        t -> Timber.e(t, "Error initializing DriveServiceHelper")));

    INSTANCE = this;
  }

  public SingleSubject<Optional<DriveServiceHelper>> driveService() {
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

  public PreferenceRepo preferenceRepo() {
    return mPreferenceRepo;
  }

  public static MyApplication cast(Application app) {
    return (MyApplication) app;
  }

  public static ViewModelFactory viewModelFactory() {
    return mViewModelFactory;
  }
}

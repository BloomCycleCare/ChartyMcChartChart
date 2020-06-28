package com.bloomcyclecare.cmcc.apps.charting;

import android.app.Application;
import android.content.Intent;

import com.bloomcyclecare.cmcc.BuildConfig;
import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.apps.ViewModelFactory;
import com.bloomcyclecare.cmcc.backup.drive.BackupWorker;
import com.bloomcyclecare.cmcc.backup.drive.PublishWorker;
import com.bloomcyclecare.cmcc.backup.drive.UpdateTrigger;
import com.bloomcyclecare.cmcc.backup.drive.WorkerManager;
import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.backup.drive.DriveServiceHelper;
import com.bloomcyclecare.cmcc.data.models.observation.Observation;
import com.bloomcyclecare.cmcc.data.models.training.Exercise;
import com.bloomcyclecare.cmcc.data.repos.DataRepos;
import com.bloomcyclecare.cmcc.data.repos.cycle.CycleRepoFactory;
import com.bloomcyclecare.cmcc.data.repos.cycle.RWCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.entry.ChartEntryRepoFactory;
import com.bloomcyclecare.cmcc.data.repos.entry.RWChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.exercise.ExerciseRepoFactory;
import com.bloomcyclecare.cmcc.data.repos.exercise.RWExerciseRepo;
import com.bloomcyclecare.cmcc.data.repos.instructions.InstructionsRepoFactory;
import com.bloomcyclecare.cmcc.data.repos.instructions.RWInstructionsRepo;
import com.bloomcyclecare.cmcc.data.repos.pregnancy.PregnancyRepoFactory;
import com.bloomcyclecare.cmcc.data.repos.pregnancy.RWPregnancyRepo;
import com.bloomcyclecare.cmcc.data.repos.sticker.RWStickerSelectionRepo;
import com.bloomcyclecare.cmcc.data.repos.sticker.StickerSelectionRepoFactory;
import com.bloomcyclecare.cmcc.logic.PreferenceRepo;
import com.bloomcyclecare.cmcc.logic.chart.ObservationParser;
import com.bloomcyclecare.cmcc.notifications.ChartingReceiver;
import com.bloomcyclecare.cmcc.ui.showcase.ShowcaseManager;
import com.bloomcyclecare.cmcc.utils.RxUtil;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.firebase.FirebaseApp;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.preference.PreferenceManager;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.SingleSubject;
import timber.log.Timber;

/**
 * Created by parkeroth on 5/15/17.
 */

public class ChartingApp extends Application implements DataRepos, WorkerManager.Provider {

  private static final ViewMode FALLBACK_VIEW_MODE = ViewMode.CHARTING;

  private static ChartingApp INSTANCE;

  private static ViewModelFactory mViewModelFactory;

  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private final SingleSubject<Optional<DriveServiceHelper>> mDriveSubject = SingleSubject.create();
  private final PublishSubject<Boolean> mManualSyncTriggers = PublishSubject.create();

  private InstructionsRepoFactory mInstructionsRepoFactory;
  private CycleRepoFactory mCycleRepoFactory;
  private ChartEntryRepoFactory mChartEntryRepoFactory;
  private PregnancyRepoFactory mPregnancyRepoFactory;
  private PreferenceRepo mPreferenceRepo;
  private StickerSelectionRepoFactory mStickerSelectionRepoFactory;
  private ExerciseRepoFactory mExerciseRepoFactory;

  private WorkerManager mWorkerManager;

  private final ShowcaseManager mShowcaseManager = new ShowcaseManager();

  public void registerDriveService(Optional<DriveServiceHelper> driveService) {
    mDriveSubject.onSuccess(driveService);
  }

  public static ChartingApp getInstance() {
    return INSTANCE;
  };

  public void triggerSync() {
    mManualSyncTriggers.onNext(true);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    FirebaseApp.initializeApp(this);
    //FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    PreferenceManager.setDefaultValues(this, R.xml.pref_settings, false);

    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree());
    }

    Migration[] migrations = new Migration[AppDatabase.MIGRATIONS.size()];
    AppDatabase.MIGRATIONS.toArray(migrations);
    //.fallbackToDestructiveMigration()  // I'm sure this will bite me in the end...
    AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "room-db")
        .addMigrations(migrations)
        //.fallbackToDestructiveMigration()  // I'm sure this will bite me in the end...
        .build();

    mInstructionsRepoFactory = new InstructionsRepoFactory(db, FALLBACK_VIEW_MODE);
    mStickerSelectionRepoFactory = new StickerSelectionRepoFactory(db, FALLBACK_VIEW_MODE);
    mExerciseRepoFactory = new ExerciseRepoFactory(FALLBACK_VIEW_MODE);
    mCycleRepoFactory = new CycleRepoFactory(db, FALLBACK_VIEW_MODE);
    mChartEntryRepoFactory = new ChartEntryRepoFactory(db, mStickerSelectionRepoFactory, FALLBACK_VIEW_MODE, (observation) -> {
      try {
        return ObservationParser.parse(observation);
      } catch (ObservationParser.InvalidObservationException ioe) {
        Timber.w(ioe );
        return java.util.Optional.empty();
      }
    });
    mPregnancyRepoFactory = new PregnancyRepoFactory(db, mCycleRepoFactory, FALLBACK_VIEW_MODE);
    mPreferenceRepo = PreferenceRepo.create(this);

    mWorkerManager = WorkerManager.create(getApplicationContext());

    mViewModelFactory = new ViewModelFactory();

    Timber.i("Sending charting reminder restart intent");
    Intent chartingRestartIntent = new Intent(this, ChartingReceiver.class);
    sendBroadcast(chartingRestartIntent);


    Flowable<UpdateTrigger> triggerStream = Flowable.merge(
        cycleRepo(ViewMode.CHARTING).updateEvents()
            .observeOn(Schedulers.computation())
            .map(e -> new UpdateTrigger(e.updateTime, e.dateRange))
            .doOnNext(t -> Timber.v("New cycle update")) ,
        entryRepo(ViewMode.CHARTING).updateEvents()
            .observeOn(Schedulers.computation())
            .flatMap(e -> cycleRepo(ViewMode.CHARTING)
                .getCycleForDate(e.updateTarget).toSingle()
                .map(cycle -> Range.closed(cycle.startDate, Optional.fromNullable(cycle.endDate).or(LocalDate.now())))
                .map(range -> new UpdateTrigger(e.updateTime, range))
                .toFlowable())
            .doOnNext(t -> Timber.v("New entry update")),
        instructionsRepo(ViewMode.CHARTING).updateEvents()
            .observeOn(Schedulers.computation())
            .map(e -> new UpdateTrigger(e.updateTime, e.dateRange))
            .doOnNext(t -> Timber.v("New instruction update")))
        .share();

    Flowable<List<UpdateTrigger>> batchedTriggers = Flowable.merge(
        triggerStream.doOnNext(t -> Timber.v("New update event"))
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
            .doOnNext(t -> Timber.v("New update batch")),
        mManualSyncTriggers.map(b -> ImmutableList.of(new UpdateTrigger(DateTime.now(), Range.singleton(LocalDate.now())))).toFlowable(BackpressureStrategy.BUFFER))
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
        }, t -> Timber.e(t, "Error creating publish request")));

    mDisposables.add(batchedTriggers
        //.startWith(ImmutableList.<UpdateTrigger>of())
        .compose(RxUtil.onceAvailable(driveService()))
        .map(trigger -> new OneTimeWorkRequest.Builder(BackupWorker.class).build())
        .compose(RxUtil.takeWhile(mPreferenceRepo.summaries(), PreferenceRepo.PreferenceSummary::backupEnabled))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(request -> {
          Timber.d("Received work request for backup");
          WorkManager.getInstance(getApplicationContext()).enqueue(request);
        }, t -> Timber.e("Error creating backup request")));

    mDisposables.add(mDriveSubject.subscribe(
        s -> Timber.d("DriveServiceHelper initialized."),
        t -> Timber.e(t, "Error initializing DriveServiceHelper")));

    INSTANCE = this;
  }

  public SingleSubject<Optional<DriveServiceHelper>> driveService() {
    return mDriveSubject;
  }

  @Override
  public RWInstructionsRepo instructionsRepo(ViewMode viewMode) {
    return mInstructionsRepoFactory.forViewMode(viewMode);
  }

  @Deprecated
  public RWInstructionsRepo instructionsRepo() {
    return instructionsRepo(ViewMode.CHARTING);
  }

  @Override
  public RWCycleRepo cycleRepo(ViewMode viewMode) {
    return mCycleRepoFactory.forViewMode(viewMode);
  }

  @Override
  public RWCycleRepo cycleRepo(Exercise exercise) {
    return mCycleRepoFactory.forExercise(exercise);
  }

  @Deprecated
  public RWCycleRepo cycleRepo() {
    return cycleRepo(ViewMode.CHARTING);
  }

  @Override
  public RWChartEntryRepo entryRepo(ViewMode viewMode) {
    return mChartEntryRepoFactory.forViewMode(viewMode);
  }

  @Override
  public RWChartEntryRepo entryRepo(Exercise exercise) {
    return mChartEntryRepoFactory.forExercise(exercise);
  }

  @Deprecated
  public RWChartEntryRepo entryRepo() {
    return entryRepo(ViewMode.CHARTING);
  }

  public PreferenceRepo preferenceRepo() {
    return mPreferenceRepo;
  }

  @Override
  public RWPregnancyRepo pregnancyRepo(ViewMode viewMode) {
    return mPregnancyRepoFactory.forViewMode(viewMode);
  }

  @Override
  public RWExerciseRepo exerciseRepo(ViewMode viewMode) {
    return mExerciseRepoFactory.forViewMode(viewMode);
  }

  @Override
  public RWStickerSelectionRepo stickerSelectionRepo(ViewMode viewMode) {
    return mStickerSelectionRepoFactory.forViewMode(viewMode);
  }

  @Override
  public RWStickerSelectionRepo stickerSelectionRepo(Exercise exercise) {
    return mStickerSelectionRepoFactory.forExercise(exercise);
  }

  @Deprecated
  public RWPregnancyRepo pregnancyRepo() {
    return pregnancyRepo(ViewMode.CHARTING);
  }

  public ShowcaseManager showcaseManager() {
    return mShowcaseManager;
  }

  public static DataRepos dataRepos(Application app) {
    return cast(app);
  }

  public static ChartingApp cast(Application app) {
    return (ChartingApp) app;
  }

  public static ViewModelFactory viewModelFactory() {
    return mViewModelFactory;
  }

  @Override
  public WorkerManager getWorkerManager() {
    return mWorkerManager;
  }
}

package com.bloomcyclecare.cmcc.ui.publish;

import android.app.Application;
import android.content.Context;
import android.widget.Toast;

import com.bloomcyclecare.cmcc.backup.drive.DriveFeaturePrefs;
import com.bloomcyclecare.cmcc.backup.drive.DriveServiceHelper;
import com.bloomcyclecare.cmcc.backup.drive.PublishWorker;
import com.bloomcyclecare.cmcc.backup.drive.WorkerManager;
import com.bloomcyclecare.cmcc.data.repos.DataRepos;
import com.bloomcyclecare.cmcc.utils.GoogleAuthHelper;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.api.services.drive.model.File;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Range;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.ReadableInstant;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.work.OneTimeWorkRequest;
import androidx.work.RxWorker;
import androidx.work.WorkerParameters;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class PublishViewModel extends AndroidViewModel {

  private final CompositeDisposable mDisposables = new CompositeDisposable();

  private final Subject<Optional<GoogleSignInAccount>> mAccountSubject = BehaviorSubject.create();
  private final Subject<Boolean> mPublishEnabledSubject = BehaviorSubject.create();
  private final Subject<Boolean> mManualTriggerSubject = PublishSubject.create();
  private final Subject<ViewState> mViewStateSubject = BehaviorSubject.create();
  private final Subject<Optional<WorkerManager.ItemStats>> mStatsSubject = BehaviorSubject.createDefault(Optional.empty());

  private final Context mContext;
  private final GoogleSignInClient mSigninClient;
  private final WorkerManager mWorkerManager;

  public PublishViewModel(@NonNull Application application) {
    super(application);
    mContext = application.getApplicationContext();
    mWorkerManager = WorkerManager.fromApp(application);
    mSigninClient = GoogleAuthHelper.getClient(mContext);

    DriveFeaturePrefs prefs = new DriveFeaturePrefs(PublishWorker.class, application);
    mStatsSubject.onNext(Optional.of(prefs.createStats()));

    mPublishEnabledSubject.onNext(prefs.getEnabled());
    mDisposables.add(mPublishEnabledSubject.subscribe(prefs::setEnabled));

    mDisposables.add(mManualTriggerSubject.subscribe(b -> {
      if (mWorkerManager.manualTrigger(WorkerManager.Item.PUBLISH, PublishWorker.forDateRange(Range.singleton(LocalDate.now())))) {
        Toast.makeText(mContext, "Manual Publish Triggered", Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(mContext, "Error Triggering Publish", Toast.LENGTH_SHORT).show();
      }
    }));

    maybeReconnectStats();

    DataRepos dataRepos = DataRepos.fromApp(application);
    Observable<OneTimeWorkRequest> workStream = dataRepos
        .updateStream(30)
        .toObservable()
        .map(PublishWorker::forDateRange);

    Observable<Optional<String>> driveFolderLinkStream = mAccountSubject.distinctUntilChanged()
        .flatMap(account -> {
          if (!account.isPresent()) {
            return Observable.just(Optional.empty());
          }
          DriveServiceHelper driveServiceHelper =
              DriveServiceHelper.forAccount(account.get(), application);
          return driveServiceHelper
              .getOrCreateFolder(DriveServiceHelper.FOLDER_NAME_MY_CHARTS)
              .map(myChartsFolder -> Optional.of(getDriveLink(myChartsFolder)))
              .toObservable();
        });

    Observable.combineLatest(
        mAccountSubject.distinctUntilChanged(),
        mPublishEnabledSubject.distinctUntilChanged(),
        mStatsSubject.distinctUntilChanged(),
        driveFolderLinkStream.distinctUntilChanged(),
        (account, publishEnabled, stats, driveFolderLink) -> {
          if (account.isPresent() && publishEnabled) {
            if (maybeConnectWorkSteam(workStream)) {
              mManualTriggerSubject.onNext(true);
            }
            stats.ifPresent(prefs::updateStats);
            Optional<Integer> itemsOutstanding = stats.map(itemStats -> itemStats.numEncueuedRequests() - itemStats.numCompletedRequests());
            Optional<ReadableInstant> lastEncueueTime = stats.flatMap(itemStats -> {
              if (itemStats.lastEncueueTime() == 0) {
                return Optional.empty();
              }
              return Optional.of(new DateTime(itemStats.lastEncueueTime(), DateTimeZone.getDefault()));
            });
            Optional<ReadableInstant> lastCompletedTime = stats.flatMap(itemStats -> {
              if (itemStats.lastCompletedTime() == 0) {
                return Optional.empty();
              }
              return Optional.of(new DateTime(itemStats.lastCompletedTime(), DateTimeZone.getDefault()));
            });
            return ViewState.create(account, driveFolderLink, true, itemsOutstanding, lastEncueueTime, lastCompletedTime);
          }
          if (!disconnectWorkStream()) {
            Timber.v("No work stream to disconnect");
          } else {
            Timber.v("Work stream disconnected");
          }
          return ViewState.create(account, Optional.empty(), false, Optional.empty(), Optional.empty(), Optional.empty());
        }).subscribe(mViewStateSubject);

    checkAccount();
  }

  private void maybeReconnectStats() {
    Optional<Observable<WorkerManager.ItemStats>> statsStream =
        mWorkerManager.getUpdateStream(WorkerManager.Item.PUBLISH);
    statsStream.ifPresent(itemStatsObservable -> itemStatsObservable.map(Optional::of)
        .subscribe(mStatsSubject));
  }

  private boolean maybeConnectWorkSteam(Observable<OneTimeWorkRequest> workStream) {
    Optional<Observable<WorkerManager.ItemStats>> statsStream =
        mWorkerManager.getUpdateStream(WorkerManager.Item.PUBLISH);
    if (statsStream.isPresent()) {
      Timber.v("Work stream already active");
      return false;
    }
    Timber.d("Connecting work stream");
    statsStream = mWorkerManager.register(WorkerManager.Item.PUBLISH, workStream, () -> {
      Timber.d("Toasting publish success");
      Toast.makeText(mContext, "Chart Published", Toast.LENGTH_SHORT).show();
    }, message -> {});
    if (statsStream.isPresent()) {
      Timber.d("Work stream connected");
      statsStream.get().map(Optional::of).subscribe(mStatsSubject);
      return true;
    }
    Timber.w("Failed to connect publish stream!");
    return false;
  }

  private boolean disconnectWorkStream() {
    if (mWorkerManager.cancel(WorkerManager.Item.PUBLISH)) {
      mStatsSubject.onNext(Optional.empty());
      return true;
    }
    return false;
  }

  public LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mViewStateSubject.toFlowable(BackpressureStrategy.BUFFER));
  }

  public Observer<Boolean> publishEnabledObserver() {
    return mPublishEnabledSubject;
  }

  public Observer<Boolean> manualTriggerObserver() {
    return mManualTriggerSubject;
  }

  public void signOut() {
    mSigninClient.signOut().addOnSuccessListener(aVoid -> checkAccount());
  }

  public void checkAccount() {
    Timber.d("Checking for account");
    GoogleAuthHelper.googleAccount(mContext)
        .map(Optional::of)
        .switchIfEmpty(Single.just(Optional.empty()))
        .subscribe(account -> mAccountSubject.onNext(account));
  }

  @AutoValue
  public static abstract class ViewState {

    public abstract Optional<GoogleSignInAccount> account();
    public abstract Optional<String> myChartsLink();
    public abstract boolean publishEnabled();
    public abstract Optional<Integer> itemsOutstanding();
    public abstract Optional<ReadableInstant> lastEncueueTimeMs();
    public abstract Optional<ReadableInstant> lastSuccessTimeMs();

    public static ViewState create(Optional<GoogleSignInAccount> account, Optional<String> myChartsLink, boolean publishEnabled, Optional<Integer> itemsOutstanding, Optional<ReadableInstant> lastEncueueTimeMs, Optional<ReadableInstant> lastSuccessTimeMs) {
      return new AutoValue_PublishViewModel_ViewState(account, myChartsLink, publishEnabled, itemsOutstanding, lastEncueueTimeMs, lastSuccessTimeMs);
    }
  }

  private static String getDriveLink(@NonNull File file) {
    return String.format("https://drive.google.com/drive/folders/%s", file.getId());
  }

  public static class NoopWorker extends  RxWorker {
    /**
     * @param appContext   The application {@link Context}
     * @param workerParams Parameters to setup the internal state of this worker
     */
    public NoopWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
      super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Single<Result> createWork() {
      Timber.d("Creating work");
      return Single.timer(10, TimeUnit.SECONDS).map(l -> Result.success());
    }
  }
}

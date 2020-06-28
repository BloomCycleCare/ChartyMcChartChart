package com.bloomcyclecare.cmcc.ui.cloud;

import android.app.Application;
import android.content.Context;

import com.bloomcyclecare.cmcc.backup.drive.DriveServiceHelper;
import com.bloomcyclecare.cmcc.backup.drive.PublishWorker;
import com.bloomcyclecare.cmcc.backup.drive.WorkerManager;
import com.bloomcyclecare.cmcc.data.repos.DataRepos;
import com.bloomcyclecare.cmcc.logic.PreferenceRepo;
import com.bloomcyclecare.cmcc.utils.GoogleAuthHelper;
import com.bloomcyclecare.cmcc.utils.RxUtil;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.api.services.drive.model.File;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.ReadableInstant;
import org.joda.time.Instant;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.LiveData;
import androidx.work.OneTimeWorkRequest;
import androidx.work.RxWorker;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.WorkerParameters;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class CloudPublishViewModel extends AndroidViewModel {

  private final Subject<Optional<GoogleSignInAccount>> mAccountSubject = BehaviorSubject.create();
  private final Subject<Boolean> mPublishEnabledSubject = BehaviorSubject.create();
  private final Subject<Boolean> mManualTriggerSubject = PublishSubject.create();
  private final Subject<ViewState> mViewStateSubject = BehaviorSubject.create();
  private final Subject<Optional<WorkerManager.ItemStats>> mStatsSubject = BehaviorSubject.createDefault(Optional.empty());

  private final Context mContext;
  private final GoogleSignInClient mSigninClient;
  private final WorkerManager mWorkerManager;

  public CloudPublishViewModel(@NonNull Application application) {
    super(application);
    mContext = application.getApplicationContext();
    mWorkerManager = WorkerManager.fromApp(application);
    mSigninClient = GoogleAuthHelper.getClient(mContext);

    DataRepos dataRepos = DataRepos.fromApp(application);

    Observable<OneTimeWorkRequest> workStream = Observable.mergeArray(
        dataRepos.updateStream(30).toObservable(),
        mManualTriggerSubject.map(t -> Range.singleton(LocalDate.now())))
        .map(dateRange -> new OneTimeWorkRequest.Builder(PublishWorker.class)
            .setInputData(PublishWorker.createInputData(dateRange))
            .build());

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
            maybeConnectWorkSteam(workStream);
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

  private void maybeConnectWorkSteam(Observable<OneTimeWorkRequest> workStream) {
    Optional<Observable<WorkerManager.ItemStats>> statsStream =
        mWorkerManager.getUpdateStream(WorkerManager.Item.PUBLISH);
    if (statsStream.isPresent()) {
      Timber.v("Work stream already active");
      return;
    }
    Timber.d("Connecting work stream");
    statsStream = mWorkerManager.register(WorkerManager.Item.PUBLISH, workStream);
    if (statsStream.isPresent()) {
      Timber.d("Work stream connected");
      statsStream.get().map(Optional::of).subscribe(mStatsSubject);
      return;
    }
    Timber.w("Failed to connect publish stream!");
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
    public abstract Boolean publishEnabled();
    public abstract Optional<Integer> itemsOutstanding();
    public abstract Optional<ReadableInstant> lastEncueueTimeMs();
    public abstract Optional<ReadableInstant> lastSuccessTimeMs();

    public static ViewState create(Optional<GoogleSignInAccount> account, Optional<String> myChartsLink, Boolean publishEnabled, Optional<Integer> itemsOutstanding, Optional<ReadableInstant> lastEncueueTimeMs, Optional<ReadableInstant> lastSuccessTimeMs) {
      return new AutoValue_CloudPublishViewModel_ViewState(account, myChartsLink, publishEnabled, itemsOutstanding, lastEncueueTimeMs, lastSuccessTimeMs);
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

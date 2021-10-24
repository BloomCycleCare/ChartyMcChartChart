package com.bloomcyclecare.cmcc.ui.restore;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.backup.AppStateImporter;
import com.bloomcyclecare.cmcc.backup.AppStateParser;
import com.bloomcyclecare.cmcc.backup.drive.BackupWorker;
import com.bloomcyclecare.cmcc.backup.drive.DriveServiceHelper;
import com.bloomcyclecare.cmcc.ui.showcase.ShowcaseManager;
import com.bloomcyclecare.cmcc.utils.GoogleAuthHelper;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.services.drive.model.File;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class RestoreFromDriveViewModel extends AndroidViewModel implements Disposable {

  private final Subject<ViewState> mViewModelSubject = BehaviorSubject.create();
  private final Subject<Optional<GoogleSignInAccount>> mAccountSubject = BehaviorSubject.create();

  @SuppressLint("StaticFieldLeak") // Disposed
  private Context mContext;
  private final ShowcaseManager mShowcaseManager;
  private final CompositeDisposable mDisposables = new CompositeDisposable();

  public RestoreFromDriveViewModel(@NonNull Application application, Activity activity) {
    super(application);
    mContext = activity;
    mShowcaseManager = ChartingApp.cast(application).showcaseManager();

    Observable<Pair<Optional<File>, Boolean>> backupFile = mAccountSubject.distinctUntilChanged()
        .flatMapSingle(account -> {
          if (!account.isPresent()) {
            Timber.v("No account, bailing out");
            return Single.just(Pair.create(Optional.empty(), false));
          }
          Timber.d(
              "Checking for backup file %s in directory %s",
              BackupWorker.filename(), BackupWorker.BACKUP_DIRECTORY_NAME_IN_DRIVE);
          DriveServiceHelper driveService = DriveServiceHelper.forAccount(account.get(), mContext);
          return driveService
              .getFolder(BackupWorker.BACKUP_DIRECTORY_NAME_IN_DRIVE)
              .flatMap(folder -> driveService.getFilesInFolder(folder, BackupWorker.filename()))
              .switchIfEmpty(Single.just(ImmutableList.of()))
              .map(files -> {
                Timber.v("Found %d files", files.size());
                if (files.isEmpty()) {
                  return Pair.create(Optional.empty(), true);
                }
                return Pair.create(Optional.of(files.get(0)), false);
              });
        });

    Observable.combineLatest(
        mAccountSubject.distinctUntilChanged(),
        backupFile,
        (account, backupFilePair) -> ViewState.create(
            account, backupFilePair.first, backupFilePair.second))
        .subscribe(mViewModelSubject);
  }

  @Override
  public void dispose() {
    mContext = null;
    mDisposables.dispose();
  }

  @Override
  public boolean isDisposed() {
    return mContext == null && mDisposables.isDisposed();
  }

  public void checkAccount() {
    Timber.d("Checking for account");
    mDisposables.add(GoogleAuthHelper.googleAccount(mContext)
        .map(Optional::of)
        .switchIfEmpty(Single.just(Optional.empty()))
        .subscribe(mAccountSubject::onNext));
  }

  public Single<Intent> switchAccount() {
    return GoogleAuthHelper.switchIntent(mContext).doOnSubscribe(d -> mAccountSubject.onNext(Optional.empty()));
  }

  public Completable restore(File backupFile) {
    return mAccountSubject.firstOrError().flatMapCompletable(account -> {
      if (!account.isPresent()) {
        throw new IllegalStateException();
      }
      DriveServiceHelper driveService = DriveServiceHelper.forAccount(account.get(), mContext);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      return driveService.downloadFile(backupFile, out)
          .map(outputStream -> out.toByteArray())
          .observeOn(Schedulers.computation())
          .flatMap(bytes -> {
            Timber.d("Parsing backup file");
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            return AppStateParser.parse(() -> in);
          })
          .flatMapCompletable(appState -> {
            Timber.d("Importing app state");
            AppStateImporter importer = new AppStateImporter(ChartingApp.getInstance());
            return importer.importAppState(appState);
          })
          .doOnComplete(() -> {
            Timber.d("Preempting all showcase prompts");
            mShowcaseManager.preemptAllMatching(p -> true);
          })
          .doOnError(Timber::e)
          .subscribeOn(Schedulers.io());
    });
  }

  public LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mViewModelSubject.toFlowable(BackpressureStrategy.BUFFER));
  }

  @AutoValue
  public static abstract class ViewState {

    abstract Optional<GoogleSignInAccount> account();
    abstract Optional<File> backupFile();
    abstract boolean noneFound();

    public static ViewState create(Optional<GoogleSignInAccount> account, Optional<File> backupFile, boolean noneFound) {
      return new AutoValue_RestoreFromDriveViewModel_ViewState(account, backupFile, noneFound);
    }

  }

  public static class Factory implements ViewModelProvider.Factory {
    private final Activity activity;

    public Factory(Activity activity) {
      this.activity = activity;
    }

    @NonNull
    @NotNull
    @Override
    public <T extends ViewModel> T create(@NonNull @NotNull Class<T> modelClass) {
      return (T) new RestoreFromDriveViewModel(activity.getApplication(), activity);
    }
  }
}

package com.bloomcyclecare.cmcc.ui.restore;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Pair;

import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.backup.AppStateImporter;
import com.bloomcyclecare.cmcc.backup.AppStateParser;
import com.bloomcyclecare.cmcc.backup.drive.BackupWorker;
import com.bloomcyclecare.cmcc.backup.drive.DriveServiceHelper;
import com.bloomcyclecare.cmcc.utils.GoogleAuthHelper;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.services.drive.model.File;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class RestoreFromDriveViewModel extends AndroidViewModel {

  private final Subject<ViewState> mViewModelSubject = BehaviorSubject.create();
  private final Subject<Optional<GoogleSignInAccount>> mAccountSubject = BehaviorSubject.create();

  private final Context mContext;

  public RestoreFromDriveViewModel(@NonNull Application application) {
    super(application);
    mContext = application.getApplicationContext();

    Observable<Pair<Optional<File>, Boolean>> backupFile = mAccountSubject.distinctUntilChanged()
        .flatMapSingle(account -> {
          if (!account.isPresent()) {
            Timber.v("No account, bailing out");
            return Single.just(Pair.create(Optional.empty(), false));
          }
          Timber.d(
              "Checking for backup file %s in directory %s",
              BackupWorker.BACKUP_FILE_NAME_IN_DRIVE, BackupWorker.BACKUP_DIRECTORY_NAME_IN_DRIVE);
          DriveServiceHelper driveService = DriveServiceHelper.forAccount(account.get(), mContext);
          return driveService
              .getFolder(BackupWorker.BACKUP_DIRECTORY_NAME_IN_DRIVE)
              .flatMap(folder -> driveService.getFilesInFolder(folder, BackupWorker.BACKUP_FILE_NAME_IN_DRIVE))
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

  public void checkAccount() {
    Timber.d("Checking for account");
    GoogleAuthHelper.googleAccount(mContext)
        .map(Optional::of)
        .switchIfEmpty(Single.just(Optional.empty()))
        .subscribe(account -> mAccountSubject.onNext(account));
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
}

package com.bloomcyclecare.cmcc.ui.restore;

import android.app.Application;
import android.content.Context;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.backup.AppStateImporter;
import com.bloomcyclecare.cmcc.backup.AppStateParser;
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
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
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

    Observable<Optional<File>> backupFile = mAccountSubject.distinctUntilChanged()
        .flatMapSingle(account -> {
          Timber.v("Checking for backup file");
          if (!account.isPresent()) {
            Timber.v("No account, bailing out");
            return Single.just(Optional.empty());
          }
          DriveServiceHelper driveService = DriveServiceHelper.forAccount(account.get(), mContext);
          return driveService
              .getFolder("My Charts")
              .flatMap(folder -> driveService.getFilesInFolder(folder, "backup.chart"))
              .switchIfEmpty(Single.just(ImmutableList.of()))
              .map(files -> files.isEmpty() ? Optional.empty() : Optional.of(files.get(0)));
        });

    Observable.combineLatest(
        mAccountSubject.distinctUntilChanged(),
        backupFile,
        ViewState::create).subscribe(mViewModelSubject);
  }

  public void checkAccount() {
    Timber.d("Checking for account");
    GoogleAuthHelper.googleAccount(mContext)
        .map(Optional::of)
        .switchIfEmpty(Single.just(Optional.empty()))
        .subscribe(account -> mAccountSubject.onNext(account));
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

    public static ViewState create(Optional<GoogleSignInAccount> account, Optional<File> backupFile) {
      return new AutoValue_RestoreFromDriveViewModel_ViewState(account, backupFile);
    }

  }
}

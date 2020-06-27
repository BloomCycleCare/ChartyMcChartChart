package com.bloomcyclecare.cmcc.ui.sharing;

import android.app.Application;
import android.content.Context;

import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.backup.drive.DriveServiceHelper;
import com.bloomcyclecare.cmcc.utils.GoogleAuthHelper;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.model.File;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.LiveData;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class SharingViewModel extends AndroidViewModel {

  private final CompositeDisposable mDisposables = new CompositeDisposable();
  private final Subject<Optional<GoogleSignInAccount>> mAccountSubject = BehaviorSubject.create();
  private final Subject<ViewState> mViewStateSubject = BehaviorSubject.create();

  private final Context mContext;
  private final GoogleSignInClient mSigninClient;

  public SharingViewModel(@NonNull Application application) {
    super(application);
    mContext = application.getApplicationContext();
    mAccountSubject.flatMapSingle(account -> {
      if (!account.isPresent()) {
        return Single.just(ViewState.create(account, Optional.empty(), ImmutableList.of()));
      }
      DriveServiceHelper driveServiceHelper =
          DriveServiceHelper.forAccount(account.get(), application);
      return driveServiceHelper
          .getOrCreateFolder(DriveServiceHelper.FOLDER_NAME_MY_CHARTS)
          .map(myChartsFolder -> ViewState.create(
              account, Optional.of(getDriveLink(myChartsFolder)), ImmutableList.of()));
    }).subscribe(mViewStateSubject);

    mSigninClient = GoogleAuthHelper.getClient(mContext);

    checkAccount();
  }

  private static String getDriveLink(@NonNull File file) {
    return String.format("https://drive.google.com/drive/folders/%s", file.getId());
  }

  public LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mViewStateSubject.toFlowable(BackpressureStrategy.BUFFER));
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
    public abstract List<File> backupFiles();

    public static ViewState create(Optional<GoogleSignInAccount> account, Optional<String> myChartsLink, List<File> backupFiles) {
      return new AutoValue_SharingViewModel_ViewState(account, myChartsLink, backupFiles);
    }

  }
}

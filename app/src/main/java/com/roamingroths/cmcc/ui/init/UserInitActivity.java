package com.roamingroths.cmcc.ui.init;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.drive.DriveScopes;
import com.google.common.base.Optional;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.drive.DriveServiceHelper;
import com.roamingroths.cmcc.utils.GoogleAuthHelper;

import java.util.concurrent.Callable;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.MaybeSubject;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;

public class UserInitActivity extends FragmentActivity {

  protected enum RequestCode {
    GET_BACKUP_LOCATION,
    FIREBASE
  }

  private UserInitializationListener mUserListener;
  private SplashFragment mFragment;

  private MaybeSubject<GoogleSignInAccount> mAccountSubject = MaybeSubject.create();
  private PublishSubject<Uri> fileLocations = PublishSubject.create();

  public Observable<Uri> fileLocation() {
    return fileLocations.hide();
  }

  // - Get FirebaseUser (or create one)
  // - Get try init Crypto and prompt if necessary

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_user_init);

    boolean importingData =
        getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW);
    if (importingData) {
      ImportAppStateFragment fragment = new ImportAppStateFragment();
      mFragment = fragment;
      mUserListener = fragment;
    } else {
      LoadCurrentCycleFragment fragment = new LoadCurrentCycleFragment();
      mFragment = fragment;
      mUserListener = fragment;
    }
    mFragment.setArguments(getIntent().getExtras());
    mFragment.updateStatus("Initializing");
    getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mFragment).commit();
    getSupportFragmentManager().executePendingTransactions();
    mFragment = (SplashFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container);

    PreferenceManager prefManager = PreferenceManager.create(this);

    CompositeDisposable disposables = new CompositeDisposable();
    disposables.add(prefManager
        .summaries()
        .firstOrError()
        .observeOn(AndroidSchedulers.mainThread())
        .flatMap(summary -> Single.<Optional<DriveServiceHelper>>merge(Single.zip(
            summary.backupEnabled(this::promptForBackup),
            summary.publishEnabled(this::promptForPublish),
            (backupEnabled, publishEnabled) -> {
              boolean needsDrive = backupEnabled || publishEnabled;
              if (!needsDrive) {
                return Single.just(Optional.absent());
              }
              return tryInitDrive(this::promptForDisablePublish, prefManager);
            })))
        .subscribe(optionalDriveService -> {
          MyApplication.cast(getApplication()).registerDriveService(optionalDriveService);
          mUserListener.onUserInitialized(null);
        }, Timber::e));
  }

  @Override
  protected void onStop() {
    super.onStop();
  }

  @Override
  protected void onStart() {
    super.onStart();
  }

  private Single<Optional<DriveServiceHelper>> tryInitDrive(Callable<Single<Boolean>> promptForDisable, PreferenceManager preferenceManager) {
    return GoogleAuthHelper.googleAccount(this)
        .switchIfEmpty(Maybe.defer(() -> promptForSignIn(this, mAccountSubject)))
        .map(account -> Optional.of(DriveServiceHelper.forAccount(account, this)))
        .switchIfEmpty(Single.defer(promptForDisable).flatMap(disable -> {
          if (disable) {
            preferenceManager.disableAllTheThings();
            return Single.just(Optional.absent());
          }
          return tryInitDrive(promptForDisable, preferenceManager);
        }));
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (resultCode == RESULT_CANCELED) {
      mAccountSubject.onComplete();
    } else {
      GoogleSignIn.getSignedInAccountFromIntent(data)
          .addOnSuccessListener(mAccountSubject::onSuccess)
          .addOnFailureListener(mAccountSubject::onError);
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  private Maybe<GoogleSignInAccount> promptForSignIn(Context context, MaybeSubject<GoogleSignInAccount> accountSubject) {
    Timber.d("Requesting sign-in");
    GoogleSignInOptions signInOptions =
        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
            .build();
    GoogleSignInClient client = GoogleSignIn.getClient(context, signInOptions);
    Intent intent = client.getSignInIntent();
    startActivityForResult(intent, 1);
    return accountSubject;
  }

  private Single<Boolean> promptForPublish() {
    return Single.create(e -> {
      new AlertDialog.Builder(UserInitActivity.this)
          .setTitle("Publish?")
          .setMessage("This will use Google Drive")
          .setPositiveButton("Yes", (dialogInterface, i) -> {
            e.onSuccess(true);
            dialogInterface.dismiss();
          })
          .setNegativeButton("No", ((dialogInterface, i) -> {
            e.onSuccess(false);
            dialogInterface.dismiss();
          }))
          .show();
    });
  }

  private Single<Boolean> promptForDisablePublish() {
    return Single.create(e -> {
      new AlertDialog.Builder(UserInitActivity.this)
          .setTitle("Disable publish?")
          .setMessage("Could not configure Drive")
          .setPositiveButton("Yes", (dialogInterface, i) -> {
            e.onSuccess(true);
            dialogInterface.dismiss();
          })
          .setNegativeButton("Retry", ((dialogInterface, i) -> {
            e.onSuccess(false);
            dialogInterface.dismiss();
          }))
          .show();
    });
  }

  private Single<Boolean> promptForBackup() {
    return Single.create(e -> {
      new AlertDialog.Builder(this)
          .setTitle("Backup?")
          .setMessage("This will use Google Drive")
          .setPositiveButton("Yes", (dialogInterface, i) -> {
            e.onSuccess(true);
            dialogInterface.dismiss();
          })
          .setNegativeButton("No", ((dialogInterface, i) -> {
            e.onSuccess(false);
            dialogInterface.dismiss();
          }))
          .show();
    });
  }

  private Single<Boolean> promptForDisableBackup() {
    return Single.create(e -> {
      boolean finishing = isFinishing();
      new AlertDialog.Builder(UserInitActivity.this)
          .setTitle("Disable backup?")
          .setMessage("Could not configure Drive")
          .setPositiveButton("Yes", (dialogInterface, i) -> {
            e.onSuccess(true);
            dialogInterface.dismiss();
          })
          .setNegativeButton("Retry", ((dialogInterface, i) -> {
            e.onSuccess(false);
            dialogInterface.dismiss();
          }))
          .show();
    });
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
  }

}

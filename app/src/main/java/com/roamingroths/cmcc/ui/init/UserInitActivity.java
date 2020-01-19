package com.roamingroths.cmcc.ui.init;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.roamingroths.cmcc.R;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class UserInitActivity extends FragmentActivity {

  protected enum RequestCode {
    GET_BACKUP_LOCATION,
    FIREBASE
  }

  private static String TAG = UserInitActivity.class.getSimpleName();

  private FirebaseAnalytics mFirebaseAnalytics;
  private UserInitializationListener mUserListener;
  private SplashFragment mFragment;

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

    mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

    final boolean importingData =
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
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mFragment).commit();
    getSupportFragmentManager().executePendingTransactions();
    mFragment = (SplashFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container);

    mFragment.updateStatus("Initializing");

    mUserListener.onUserInitialized(null);
  }

}

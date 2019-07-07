package com.roamingroths.cmcc.ui.init;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.roamingroths.cmcc.R;

import static com.roamingroths.cmcc.ui.entry.list.ChartEntryListActivity.RC_SIGN_IN;

public class UserInitActivity extends FragmentActivity {

  private static String TAG = UserInitActivity.class.getSimpleName();

  private FirebaseAnalytics mFirebaseAnalytics;
  private UserInitializationListener mUserListener;
  private SplashFragment mFragment;

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

    /*FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
      mFragment.updateStatus("Launching login");
      startActivityForResult(
          AuthUI.getInstance().createSignInIntentBuilder()
              .setAvailableProviders(Arrays.asList(
                  new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()
              ))
              .setIsSmartLockEnabled(false)
              .build(),
          RC_SIGN_IN);
    } else {
      mUserListener.onUserInitialized(user);
    }*/
    mUserListener.onUserInitialized(null);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case RC_SIGN_IN:
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
          mFragment.showError("Could not create user.");
        } else {
          mFragment.updateStatus("Login successful");
          mUserListener.onUserInitialized(user);
        }
        break;
      default:
        Log.w(UserInitActivity.class.getName(), "Unknown request code: " + requestCode);
    }
  }
}

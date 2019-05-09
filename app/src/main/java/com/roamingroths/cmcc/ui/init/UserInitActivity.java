package com.roamingroths.cmcc.ui.init;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.providers.CryptoProvider;

import java.util.Arrays;

import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static com.roamingroths.cmcc.ui.entry.list.ChartEntryListActivity.RC_SIGN_IN;

public class UserInitActivity extends FragmentActivity {

  private static String TAG = UserInitActivity.class.getSimpleName();

  private FirebaseAnalytics mFirebaseAnalytics;
  private CryptoProvider mCryptoProvider;
  private UserInitializationListener mUserListener;
  private SplashFragment mFragment;

  // - Get FirebaseUser (or create one)
  // - Get try init Crypto and prompt if necessary

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_user_init);

    mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

    mCryptoProvider = new CryptoProvider(FirebaseDatabase.getInstance());

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

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
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
      initUserState(user);
    }
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
          initUserState(user);
        }
        break;
      default:
        Log.w(UserInitActivity.class.getName(), "Unknown request code: " + requestCode);
    }
  }

  /*@Override
  protected void onSaveInstanceState(Bundle outState) {
    //No call for super(). Bug on API Level > 11.
  }*/

  private void initUserState(final FirebaseUser user) {
    MyApplication.cast(getApplication()).initProviders(user, promptForPhoneNumber(), this)
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action() {
          @Override
          public void run() throws Exception {
            mUserListener.onUserInitialized(user);
          }
        }, new Consumer<Throwable>() {
          @Override
          public void accept(Throwable throwable) throws Exception {
            Log.w(TAG, throwable.getMessage(), throwable);
          }
        });
  }

  private Maybe<String> promptForPhoneNumber() {
    return Maybe.create(new MaybeOnSubscribe<String>() {
      @Override
      public void subscribe(final @NonNull MaybeEmitter<String> e) throws Exception {
        AlertDialog.Builder builder = new AlertDialog.Builder(UserInitActivity.this);
        builder.setTitle("Current Phone Number");
        builder.setMessage("This is used to protect your backups and is not stored by the app. To migrate your data to another device you will need to log in with the same account and provide this number to access your data.");
        builder.setIcon(R.drawable.ic_key_black_24dp);
        final EditText input = new EditText(UserInitActivity.this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        builder.setView(input);
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
          public void onClick(final DialogInterface dialog, int whichButton) {
            // TODO: format and validate number
            String phoneNumberStr = input.getText().toString();
            e.onSuccess(phoneNumberStr);
          }
        });
        builder.create().show();
      }
    }).subscribeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe(new Consumer<Disposable>() {
          @Override
          public void accept(Disposable disposable) throws Exception {
            Log.v("UserInitActivity", "Prompting for phone number");
          }
        })
        .doOnSuccess(new Consumer<String>() {
          @Override
          public void accept(String s) throws Exception {
            Log.v("UserInitActivity", "Phone number: " + s);
          }
        });
  }
}

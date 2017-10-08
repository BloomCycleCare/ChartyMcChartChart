package com.roamingroths.cmcc.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.crypto.CyrptoExceptions;
import com.roamingroths.cmcc.data.UserInitializationListener;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.Listeners;

import java.util.HashMap;
import java.util.Map;

import static com.roamingroths.cmcc.ui.entry.list.ChartEntryListActivity.RC_SIGN_IN;

public class UserInitActivity extends FragmentActivity {

  private UserInitializationListener mUserListener;
  private SplashFragment mFragment;

  // - Get FirebaseUser (or create one)
  // - Get try init Crypto and prompt if necessary

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_user_init);

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
    getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mFragment).commit();
    getSupportFragmentManager().executePendingTransactions();

    mFragment.updateStatus("Loading user");

    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
      mFragment.updateStatus("Launching login");
      startActivityForResult(
          AuthUI.getInstance().createSignInIntentBuilder()
              .setProviders(AuthUI.GOOGLE_PROVIDER)
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
          initUserState(user);
        }
        break;
      default:
        Log.w(UserInitActivity.class.getName(), "Unknown request code: " + requestCode);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    //No call for super(). Bug on API Level > 11.
  }

  private abstract class ErrorPrintingCallback<T> implements Callbacks.Callback<T> {
    @Override
    public void handleError(DatabaseError error) {
      Log.e("UserInitActivity", error.getMessage());
      mFragment.showError(error.getMessage());
    }
  }

  private void createUserDbEntry(final FirebaseUser user, final Callbacks.Callback<Void> doneCallback) {
    mFragment.showProgress("Creating new user");
    promptForPhoneNumber(new ErrorPrintingCallback<String>() {
      @Override
      public void acceptData(String phoneNumberStr) {
        try {
          mFragment.showProgress("Initializing crypto");
          CryptoUtil.init();
          FirebaseDatabase db = FirebaseDatabase.getInstance();
          final DatabaseReference userRef = db.getReference("users").child(user.getUid());
          Map<String, Object> updates = new HashMap<>();
          updates.put("display-name", user.getDisplayName());
          updates.put("pub-key", CryptoUtil.getPublicKeyStr());
          updates.put("private-key", CryptoUtil.getWrappedPrivateKeyStr(phoneNumberStr));
          userRef.updateChildren(updates, Listeners.completionListener(doneCallback, new Runnable() {
            @Override
            public void run() {
              mFragment.showProgress("Successfuly stored user in DB");
              doneCallback.acceptData(null);
            }
          }));
          mFragment.showProgress("Storing user in DB");
        } catch (CyrptoExceptions.CryptoException ce) {
          doneCallback.handleError(DatabaseError.fromException(ce));
        }
      }

      @Override
      public void handleNotFound() {
        throw new IllegalStateException();
      }
    });
  }

  private void initUserState(final FirebaseUser user) {
    // TODO: move off UI thread start
    final Callbacks.Callback<Void> doneCallback = new ErrorPrintingCallback<Void>() {
      @Override
      public void acceptData(Void unused) {
        mFragment.showProgress("User initialization complete");
        mUserListener.onUserInitialized(user);
        // TODO: move off UI thread end
      }

      @Override
      public void handleNotFound() {
        throw new IllegalStateException();
      }
    };
    Log.v("UserInitActivity", "Initializing user state");
    DatabaseReference userRef =
        FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
    userRef.keepSynced(true);
    userRef.addListenerForSingleValueEvent(
        new Listeners.SimpleValueEventListener(doneCallback) {
          @Override
          public void onDataChange(DataSnapshot dataSnapshot) {
            if (dataSnapshot.getChildrenCount() == 0) {
              Log.v("UserInitActivity", "No user entry found in DB");
              createUserDbEntry(user, doneCallback);
            } else {
              // Found user in DB
              Log.v("UserInitActivity", "Found user entry in DB: " + dataSnapshot.getKey());
              if (CryptoUtil.initFromKeyStore()) {
                Log.v("UserInitActivity", "Crypto initialized from KeyStore");
                doneCallback.acceptData(null);
                return;
              }
              final String publicKeyStr = dataSnapshot.child("pub-key").getValue(String.class);
              final String privateKeyStr = dataSnapshot.child("private-key").getValue(String.class);
              promptForPhoneNumber(new ErrorPrintingCallback<String>() {
                @Override
                public void acceptData(String phoneNumberStr) {
                  try {
                    Log.v("UserInitActivity", "Initializing crypto decoding");
                    CryptoUtil.init(publicKeyStr, privateKeyStr, phoneNumberStr);
                    doneCallback.acceptData(null);
                  } catch (CyrptoExceptions.CryptoException ce) {
                    handleError(DatabaseError.fromException(ce));
                  }
                }

                @Override
                public void handleNotFound() {
                  throw new IllegalStateException();
                }
              });
            }
          }
        });
  }

  private void promptForPhoneNumber(final Callbacks.Callback<String> callback) {
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
        callback.acceptData(phoneNumberStr);
      }
    });
    Log.v("UserInitActivity", "Prompting for phone number");
    builder.create().show();
  }
}

package com.roamingroths.cmcc.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
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
import com.google.firebase.database.ValueEventListener;
import com.roamingroths.cmcc.Preferences;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.crypto.CyrptoExceptions;
import com.roamingroths.cmcc.data.AppState;
import com.roamingroths.cmcc.data.CycleProvider;
import com.roamingroths.cmcc.data.EntryContainerList;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.ui.entry.list.ChartEntryListActivity;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.Listeners;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.joda.time.LocalDate;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.roamingroths.cmcc.ui.entry.list.ChartEntryListActivity.RC_SIGN_IN;

public class SplashActivity extends FragmentActivity {

  private CycleProvider mCycleProvider;

  private Preferences mPreferences;

  private SplashFragment mFragment;

  // - Get FirebaseUser (or create one)
  // - Get try init Crypto and prompt if necessary

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash);

    mPreferences = Preferences.fromShared(getApplicationContext());
    mCycleProvider = CycleProvider.forDb(FirebaseDatabase.getInstance());

    mFragment = (SplashFragment) getSupportFragmentManager().findFragmentById(R.id.splash_fragment);

    mFragment.showProgress("Loading user account");

    // Get user
    final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
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

  private Callbacks.Callback<Void> userInitCompleteCallback(final FirebaseUser user) {
    return new ErrorPrintingCallback<Void>() {

      @Override
      public void acceptData(Void unused) {
        mFragment.showProgress("User initialization complete");
        getCurrentCycleForUser(user);
        // TODO: move off UI thread end
      }

      @Override
      public void handleNotFound() {
        throw new IllegalStateException();
      }
    };
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
        Log.w(SplashActivity.class.getName(), "Unknown request code: " + requestCode);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    //No call for super(). Bug on API Level > 11.
  }

  private void preloadCycleData(final Cycle cycle) {
    log("Preload cycle data: start");
    mFragment.updateStatus("Decrypting cycle data");
    EntryContainerList.builder(cycle, mPreferences).build().initialize(mCycleProvider, Callbacks.singleUse(new Callbacks.Callback<Void>() {
      @Override
      public void acceptData(Void data) {
        log("Preload cycle data: finish");
        Intent intent = new Intent(getApplicationContext(), ChartEntryListActivity.class);
        intent.putExtra(Cycle.class.getName(), cycle);
        finish();
        startActivity(intent);
      }

      @Override
      public void handleNotFound() {
        mFragment.showError("Could not decrypt cycle entries.");
        new AlertDialog.Builder(SplashActivity.this)
            //set message, title, and icon
            .setTitle("Delete All Cycles?")
            .setMessage("This is permanent and cannot be undone!")
            .setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
              public void onClick(final DialogInterface dialog, int whichButton) {
                mCycleProvider.dropCycles(new Callbacks.HaltingCallback<Void>() {
                  @Override
                  public void acceptData(Void data) {
                    dialog.dismiss();
                    getCurrentCycleForUser(FirebaseAuth.getInstance().getCurrentUser());
                  }
                });
              }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                mFragment.showError("Please restart the app.");
                dialog.dismiss();
              }
            })
            .create().show();
      }

      @Override
      public void handleError(DatabaseError error) {
        mFragment.showError(error.getMessage());
      }
    }));
  }

  private void promptForStartOfCurrentCycle(final FirebaseUser user) {
    mFragment.updateStatus("Prompting for start of first cycle.");
    mFragment.updateStatus("Creating first cycle");
    DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
        new DatePickerDialog.OnDateSetListener() {
          @Override
          public void onDateSet(
              DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
            LocalDate cycleStartDate = new LocalDate(year, monthOfYear + 1, dayOfMonth);
            log("Creating new cycle starting " + cycleStartDate.toString());
            Callbacks.Callback<Cycle> cycleCallback = new Callbacks.HaltingCallback<Cycle>() {
              @Override
              public void acceptData(final Cycle cycle) {
                log("Done creating new cycle");
                preloadCycleData(cycle);
              }
            };
            Cycle previousCycle = null;
            Cycle nextCycle = null;
            LocalDate cycleEndDate = null;
            mCycleProvider.createCycle(
                user.getUid(),
                previousCycle,
                nextCycle,
                cycleStartDate,
                cycleEndDate,
                cycleCallback);
          }
        });
    datePickerDialog.setTitle("First day of current cycle");
    datePickerDialog.setMaxDate(Calendar.getInstance());
    datePickerDialog.show(getFragmentManager(), "datepickerdialog");
  }

  private void importDataFromIntent(Intent intent, String userId) {
    Uri uri = intent.getData();
    Log.v("SplashActivity", "Reading data from " + uri.getPath());
    try {
      InputStream in = getContentResolver().openInputStream(uri);
      AppState.parseAndPushToDB(in, userId, mCycleProvider, new Callbacks.HaltingCallback<Cycle>() {
        @Override
        public void acceptData(Cycle cycle) {
          preloadCycleData(cycle);
        }
      });
    } catch (FileNotFoundException e) {
      mFragment.showError("File " + uri.getPath() + " does not exist");
      return;
    }
  }

  private void confirmImport(final Callbacks.Callback<Boolean> callback) {
    new AlertDialog.Builder(SplashActivity.this)
        //set message, title, and icon
        .setTitle("Import data from file?")
        .setMessage("This will wipe all existing data load the data from the file. This is permanent and cannot be undone!")
        .setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
          public void onClick(final DialogInterface dialog, int whichButton) {
            dialog.dismiss();
            callback.acceptData(true);
          }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            callback.acceptData(false);
          }
        })
        .create().show();
  }

  private void getCurrentCycleForUser(final FirebaseUser user) {
    mFragment.updateStatus("Fetching current cycle");
    final Intent intent = getIntent();
    final boolean importingData =
        intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW);
    if (importingData) {
      Log.v("SplashActivity", "Importing data from file");
    }
    mCycleProvider.getCurrentCycle(user.getUid(), new Callbacks.Callback<Cycle>() {
      @Override
      public void acceptData(Cycle cycle) {
        mFragment.updateStatus("Received current cycle from DB.");
        if (importingData) {
          Log.v("SplashActivity", "Confirming data wipe");
          confirmImport(new Callbacks.SwitchingCallback() {
            @Override
            public void positive() {
              importDataFromIntent(intent, user.getUid());
            }

            @Override
            public void negative() {
              finish();
            }
          });
        } else {
          preloadCycleData(cycle);
        }
      }

      @Override
      public void handleNotFound() {
        mFragment.updateStatus("No cycle found in DB.");
        if (importingData) {
          importDataFromIntent(intent, user.getUid());
        } else {
          promptForStartOfCurrentCycle(user);
        }
      }

      @Override
      public void handleError(DatabaseError error) {
        mFragment.showError("Error fetching current cycle.");
        error.toException().printStackTrace();
      }
    });
  }

  private abstract class ErrorPrintingListener implements ValueEventListener {
    @Override
    public final void onCancelled(DatabaseError error) {
      Log.e("SplashActivity", error.getMessage());
      mFragment.showError(error.getMessage());
    }
  }

  private abstract class ErrorPrintingCallback<T> implements Callbacks.Callback<T> {
    @Override
    public void handleError(DatabaseError error) {
      Log.e("SplashActivity", error.getMessage());
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
    final Callbacks.Callback<Void> doneCallback = userInitCompleteCallback(user);
    Log.v("SplashActivity", "Initializing user state");
    DatabaseReference userRef =
        FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
    userRef.keepSynced(true);
    userRef.addListenerForSingleValueEvent(
        new ErrorPrintingListener() {
          @Override
          public void onDataChange(DataSnapshot dataSnapshot) {
            if (dataSnapshot.getChildrenCount() == 0) {
              Log.v("SplashActivity", "No user entry found in DB");
              createUserDbEntry(user, doneCallback);
            } else {
              // Found user in DB
              Log.v("SplashActivity", "Found user entry in DB: " + dataSnapshot.getKey());
              if (CryptoUtil.initFromKeyStore()) {
                Log.v("SplashActivity", "Crypto initialized from KeyStore");
                doneCallback.acceptData(null);
                return;
              }
              final String publicKeyStr = dataSnapshot.child("pub-key").getValue(String.class);
              final String privateKeyStr = dataSnapshot.child("private-key").getValue(String.class);
              promptForPhoneNumber(new ErrorPrintingCallback<String>() {
                @Override
                public void acceptData(String phoneNumberStr) {
                  try {
                    Log.v("SplashActivity", "Initializing crypto decoding");
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
    AlertDialog.Builder builder = new AlertDialog.Builder(SplashActivity.this);
    builder.setTitle("Current Phone Number");
    builder.setMessage("This is used to protect your backups and is not stored by the app. To migrate your data to another device you will need to log in with the same account and provide this number to access your data.");
    builder.setIcon(R.drawable.ic_key_black_24dp);
    final EditText input = new EditText(SplashActivity.this);
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
    Log.v("SplashActivity", "Prompting for phone number");
    builder.create().show();
  }

  private void log(String message) {
    Log.v(SplashActivity.class.getName(), message);
  }
}

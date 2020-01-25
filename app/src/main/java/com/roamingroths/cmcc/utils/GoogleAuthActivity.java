package com.roamingroths.cmcc.utils;

import android.app.Activity;
import android.content.Intent;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.drive.DriveScopes;

import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.subjects.SingleSubject;
import timber.log.Timber;

public abstract class GoogleAuthActivity extends AppCompatActivity {

  private static final int REQUEST_CODE_SIGN_IN = 1;

  private final SingleSubject<GoogleSignInAccount> signInSubject = SingleSubject.create();

  protected Single<GoogleSignInAccount> googleAccount() {
    return Maybe.<GoogleSignInAccount>create(e -> {
      GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
      if (account == null) {
        e.onSuccess(account);
      } else {
        requestSignIn();
        e.onComplete();
      }
    }).switchIfEmpty(signInSubject);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CODE_SIGN_IN:
        if (resultCode == Activity.RESULT_OK && data != null) {
          handleSignInResult(data);
        }
        break;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  /**
   * Starts a sign-in activity using {@link #REQUEST_CODE_SIGN_IN}.
   */
  private void requestSignIn() {
    Timber.d("Requesting sign-in");

    GoogleSignInOptions signInOptions =
        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
            .build();
    GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

    // The result of the sign-in Intent is handled in onActivityResult.
    startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
  }

  /**
   * Handles the {@code result} of a completed sign-in activity initiated from {@link
   * #requestSignIn()}.
   */
  private void handleSignInResult(Intent result) {
    Single.<GoogleSignInAccount>create(e -> {
      GoogleSignIn.getSignedInAccountFromIntent(result)
          .addOnSuccessListener(e::onSuccess)
          .addOnFailureListener(e::onError);
    }).subscribe(signInSubject);
  }
}

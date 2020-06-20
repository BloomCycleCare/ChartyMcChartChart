package com.bloomcyclecare.cmcc.utils;

import android.content.Context;
import android.content.Intent;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.drive.DriveScopes;

import io.reactivex.Maybe;
import io.reactivex.subjects.MaybeSubject;
import timber.log.Timber;

import static android.app.Activity.RESULT_CANCELED;

public class GoogleAuthHelper {

  public static Maybe<GoogleSignInAccount> googleAccount(Context context) {
    return lastSignedInAccount(context);
  }

  private static Maybe<GoogleSignInAccount> lastSignedInAccount(Context context) {
    return Maybe.create(e -> {
      Timber.v("Getting previous signin account");
      GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
      if (account == null) {
        e.onComplete();
      } else {
        Timber.v("Found previous account");
        e.onSuccess(account);
      }
    });
  }

  public static Intent getPromptIntent(Context context) {
    GoogleSignInOptions signInOptions =
        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
            .build();
    GoogleSignInClient client = GoogleSignIn.getClient(context, signInOptions);
    return client.getSignInIntent();
  }

  public static void handlePromptResponse(int responseCode, Intent data, MaybeSubject<GoogleSignInAccount> accountSubject) {
    if (responseCode == RESULT_CANCELED) {
      accountSubject.onComplete();
    } else {
      GoogleSignIn.getSignedInAccountFromIntent(data)
          .addOnSuccessListener(accountSubject::onSuccess)
          .addOnFailureListener(accountSubject::onError);
    }
  }

}

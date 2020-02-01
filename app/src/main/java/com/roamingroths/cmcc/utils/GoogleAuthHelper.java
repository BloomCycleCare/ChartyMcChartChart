package com.roamingroths.cmcc.utils;

import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import io.reactivex.Maybe;
import timber.log.Timber;

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

}

package com.roamingroths.cmcc.application;

import android.app.Application;
import android.support.v7.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.data.CryptoProvider;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;

/**
 * Created by parkeroth on 5/15/17.
 */

public class FirebaseApplication extends Application {

  private static CryptoUtil mCryptoUtil;

  @Override
  public void onCreate() {
    super.onCreate();
    Security.addProvider(new BouncyCastleProvider());

    FirebaseDatabase.getInstance().setPersistenceEnabled(true);

    PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
  }

  public static Single<CryptoUtil> initCryptoUtil(FirebaseUser user, Maybe<String> phoneNumber) {
    return CryptoProvider.forDb(FirebaseDatabase.getInstance()).createCryptoUtil(user, phoneNumber)
        .doOnSuccess(new Consumer<CryptoUtil>() {
          @Override
          public void accept(CryptoUtil cryptoUtil) throws Exception {
            mCryptoUtil = cryptoUtil;
          }
        });
  }

  public static CryptoUtil getCryptoUtil() {
    return mCryptoUtil;
  }
}

package com.roamingroths.cmcc.application;

import android.app.Application;
import android.support.v7.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.crypto.RxCryptoUtil;
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

  private static RxCryptoUtil mCryptoUtil;

  @Override
  public void onCreate() {
    super.onCreate();
    Security.addProvider(new BouncyCastleProvider());

    FirebaseDatabase.getInstance().setPersistenceEnabled(true);

    PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
  }

  public static Single<RxCryptoUtil> initCryptoUtil(FirebaseUser user, Maybe<String> phoneNumber) {
    return CryptoProvider.forDb(FirebaseDatabase.getInstance()).createCryptoUtil(user, phoneNumber)
        .doOnSuccess(new Consumer<RxCryptoUtil>() {
          @Override
          public void accept(RxCryptoUtil cryptoUtil) throws Exception {
            mCryptoUtil = cryptoUtil;
          }
        });
  }

  public static RxCryptoUtil getCryptoUtil() {
    return mCryptoUtil;
  }
}

package com.roamingroths.cmcc;

import android.app.Application;
import android.support.v7.preference.PreferenceManager;

import com.google.firebase.database.FirebaseDatabase;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

/**
 * Created by parkeroth on 5/15/17.
 */

public class FirebaseApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    Security.addProvider(new BouncyCastleProvider());

    FirebaseDatabase.getInstance().setPersistenceEnabled(true);

    PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
  }
}

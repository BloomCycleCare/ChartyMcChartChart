package com.roamingroths.cmcc.ui;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.application.FirebaseApplication;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.data.CycleProvider;

/**
 * Created by parkeroth on 11/12/17.
 */

public abstract class BaseActivity extends AppCompatActivity {

  private CryptoUtil mCryptoUtil;
  private Providers mProviders;
  private FirebaseUser mUser;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
    super.onCreate(savedInstanceState, persistentState);

    mUser = FirebaseAuth.getInstance().getCurrentUser();
    mCryptoUtil = FirebaseApplication.getCryptoUtil();
    mProviders = new Providers(FirebaseDatabase.getInstance(), mCryptoUtil);
  }

  Providers getProvider() {
    return mProviders;
  }

  FirebaseUser getUser() {
    return mUser;
  }

  public static class Providers {
    private final CycleProvider mCycleProvider;

    Providers(FirebaseDatabase db, CryptoUtil cryptoUtil) {
      mCycleProvider = CycleProvider.forDb(db, cryptoUtil);
    }

    public CycleProvider forCycle() {
      return mCycleProvider;
    }
  }
}

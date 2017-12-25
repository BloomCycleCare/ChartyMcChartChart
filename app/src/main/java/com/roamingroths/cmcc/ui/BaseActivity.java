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
import com.roamingroths.cmcc.data.ChartEntryProvider;
import com.roamingroths.cmcc.data.CycleEntryProvider;
import com.roamingroths.cmcc.data.CycleProvider;

/**
 * Created by parkeroth on 11/12/17.
 */

public abstract class BaseActivity extends AppCompatActivity {

  private CryptoUtil mCryptoUtil;
  private Providers mProviders;
  private FirebaseUser mUser;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

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
    private final CycleEntryProvider mCycleEntryProvider;
    private final ChartEntryProvider mChartEntryProvider;

    Providers(FirebaseDatabase db, CryptoUtil cryptoUtil) {
      mCycleProvider = CycleProvider.forDb(db, cryptoUtil);
      mChartEntryProvider = new ChartEntryProvider(db, cryptoUtil);
      mCycleEntryProvider = new CycleEntryProvider(mCycleProvider, mChartEntryProvider);
    }

    public CycleEntryProvider forCycleEntry() {
      return mCycleEntryProvider;
    }

    public CycleProvider forCycle() {
      return mCycleProvider;
    }

    public ChartEntryProvider forChartEntry() {
      return mChartEntryProvider;
    }
  }
}

package com.roamingroths.cmcc.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.roamingroths.cmcc.application.MyApplication;

/**
 * Created by parkeroth on 11/12/17.
 */

public abstract class BaseActivity extends AppCompatActivity {

  private FirebaseUser mUser;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mUser = FirebaseAuth.getInstance().getCurrentUser();
  }

  MyApplication.Providers getProvider() {
    return MyApplication.getProviders();
  }

  FirebaseUser getUser() {
    return mUser;
  }

}

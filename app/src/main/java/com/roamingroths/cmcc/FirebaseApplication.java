package com.roamingroths.cmcc;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by parkeroth on 5/15/17.
 */

public class FirebaseApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    FirebaseDatabase.getInstance().setPersistenceEnabled(true);
  }
}

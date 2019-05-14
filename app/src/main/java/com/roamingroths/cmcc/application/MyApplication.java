package com.roamingroths.cmcc.application;

import android.app.Application;

import androidx.preference.PreferenceManager;
import androidx.room.Room;
import androidx.room.migration.Migration;

import com.google.firebase.FirebaseApp;
import com.roamingroths.cmcc.BuildConfig;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.db.AppDatabase;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

import timber.log.Timber;

/**
 * Created by parkeroth on 5/15/17.
 */

public class MyApplication extends Application {

  private static ViewModelFactory mViewModelFactory;

  private AppDatabase mDB;

  @Override
  public void onCreate() {
    super.onCreate();
    FirebaseApp.initializeApp(this);
    //FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    Security.addProvider(new BouncyCastleProvider());
    PreferenceManager.setDefaultValues(this, R.xml.pref_settings, false);

    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree());
    }

    Migration[] migrations = new Migration[AppDatabase.MIGRATIONS.size()];
    AppDatabase.MIGRATIONS.toArray(migrations);
    mDB = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "room-db")
        .addMigrations(migrations)
        .fallbackToDestructiveMigration()  // I'm sure this will bite me in the end...
        .build();

    mViewModelFactory = new ViewModelFactory();
  }

  public AppDatabase db() {
    return mDB;
  }

  public static MyApplication cast(Application app) {
    return (MyApplication) app;
  }

  public static ViewModelFactory viewModelFactory() {
    return mViewModelFactory;
  }
}

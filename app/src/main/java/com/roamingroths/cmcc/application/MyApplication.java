package com.roamingroths.cmcc.application;

import android.app.Application;
import android.content.Intent;

import androidx.preference.PreferenceManager;
import androidx.room.Room;
import androidx.room.migration.Migration;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.firebase.FirebaseApp;
import com.roamingroths.cmcc.BuildConfig;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.backup.AppStateExporter;
import com.roamingroths.cmcc.data.db.AppDatabase;
import com.roamingroths.cmcc.data.repos.ChartEntryRepo;
import com.roamingroths.cmcc.data.repos.CycleRepo;
import com.roamingroths.cmcc.data.repos.InstructionsRepo;
import com.roamingroths.cmcc.notifications.ChartingReceiver;
import com.roamingroths.cmcc.utils.GsonUtil;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.security.Security;

import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

/**
 * Created by parkeroth on 5/15/17.
 */

public class MyApplication extends Application {

  private static ViewModelFactory mViewModelFactory;

  private final CompositeDisposable mDisposables = new CompositeDisposable();

  private AppDatabase mDB;
  private InstructionsRepo mInstructionsRepo;
  private CycleRepo mCycleRepo;
  private ChartEntryRepo mChartEntryRepo;

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
        //.fallbackToDestructiveMigration()  // I'm sure this will bite me in the end...
        .build();

    mInstructionsRepo = new InstructionsRepo(this);
    mChartEntryRepo = new ChartEntryRepo(mDB);
    mCycleRepo = new CycleRepo(mDB);

    mViewModelFactory = new ViewModelFactory();

    Timber.i("Sending charting reminder restart intent");
    Intent restartIntent = new Intent(this, ChartingReceiver.class);
    sendBroadcast(restartIntent);

    AppStateExporter exporter = new AppStateExporter(this);
    mDisposables.add(exporter
        .export()
        .map(appState -> GsonUtil.getGsonInstance().toJson(appState))
        .subscribe(json -> {
          File path = new File(this.getFilesDir(), "backup/");
          if (!path.exists()) {
            path.mkdir();
          }
          File file = new File(path, "cmcc_export.chart");

          Files.write(json, file, Charsets.UTF_8);
        }));

  }

  @Deprecated
  public AppDatabase db() {
    return mDB;
  }

  public InstructionsRepo instructionsRepo() {
    return mInstructionsRepo;
  }

  public CycleRepo cycleRepo() {
    return mCycleRepo;
  }

  public ChartEntryRepo entryRepo() {
    return mChartEntryRepo;
  }

  public static MyApplication cast(Application app) {
    return (MyApplication) app;
  }

  public static ViewModelFactory viewModelFactory() {
    return mViewModelFactory;
  }
}

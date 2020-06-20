package com.bloomcyclecare.cmcc.notifications;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;

import androidx.annotation.Nullable;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

public class SyncService extends Service {

  private final CompositeDisposable d = new CompositeDisposable();


  @Override
  public void onCreate() {
    super.onCreate();
    Timber.i("Creating SyncService");

    ChartingApp myApp = ChartingApp.cast(getApplication());
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}

package com.bloomcyclecare.cmcc.backup.drive;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class DriveFeaturePrefs {

  private enum PrefKeys {
    ENABLED,
    TIME_LAST_COMPLETED_MS,
    TIME_LAST_TRIGGERED_MS
  }

  private final SharedPreferences mPreferences;

  public DriveFeaturePrefs(Class<?> workerClazz, Application application) {
    mPreferences = application.getSharedPreferences(
        workerClazz.getCanonicalName(), Context.MODE_PRIVATE);
  }

  public boolean getEnabled() {
    return mPreferences.getBoolean(PrefKeys.ENABLED.name(), false);
  }

  public boolean setEnabled(boolean value) {
    return mPreferences.edit().putBoolean(PrefKeys.ENABLED.name(), value).commit();
  }

  public boolean updateStats(WorkerManager.ItemStats stats) {
    return mPreferences.edit()
        .putLong(PrefKeys.TIME_LAST_COMPLETED_MS.name(), stats.lastCompletedTime())
        .putLong(PrefKeys.TIME_LAST_TRIGGERED_MS.name(), stats.lastEncueueTime())
        .commit();
  }

  public WorkerManager.ItemStats createStats() {
    return WorkerManager.ItemStats.create(
       0,
       0,
        mPreferences.getLong(PrefKeys.TIME_LAST_COMPLETED_MS.name(), 0L),
        mPreferences.getLong(PrefKeys.TIME_LAST_TRIGGERED_MS.name(), 0L));
  }
}

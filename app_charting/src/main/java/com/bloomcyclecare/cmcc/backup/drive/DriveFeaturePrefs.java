package com.bloomcyclecare.cmcc.backup.drive;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class DriveFeaturePrefs {

  private enum PrefKeys {
    ENABLED
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
}

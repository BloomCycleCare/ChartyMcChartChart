package com.bloomcyclecare.cmcc.logic;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.Callable;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

public class PreferenceRepo {

  private enum Key {
    PROMPTED_FOR_BACKUP,
    PROMPTED_FOR_PUBLISH,
    CHARTING_REMINDER,
    ENABLE_BACKUP_TO_DRIVE,
    ENABLE_PUBLISH_CHARTS_TO_DRIVE,
    DEFAULT_TO_DEMO,
    DEFAULT_TO_GRID,
    AUTO_STICKERING,
    ENABLE_LH_TEST_MEASUREMENTS,
    ENABLE_CLEARBLUE_MACHINE_MEASUREMENTS
  }

  private  final BehaviorSubject<PreferenceSummary> mSummarySubject = BehaviorSubject.create();
  private final SharedPreferences preferences;
  private final SharedPreferences.OnSharedPreferenceChangeListener mListener;

  public static PreferenceRepo create(Context context) {
    return new PreferenceRepo(context);
  }

  private PreferenceRepo(Context context) {
    preferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
    mListener = (prefs, s) -> {
      Timber.d("Received preference update");
      for (Key key : Key.values()) {
        if (key.name().equals(s)) {
          Timber.d("Triggering preference refresh");
          mSummarySubject.onNext(new PreferenceSummary(prefs));
          break;
        }
      }
    };
    preferences.registerOnSharedPreferenceChangeListener(mListener);
    mSummarySubject.onNext(new PreferenceSummary(preferences));
  }

  public PreferenceSummary currentSummary() {
    return mSummarySubject.getValue();
  }

  public Flowable<PreferenceSummary> summaries() {
    return mSummarySubject.toFlowable(BackpressureStrategy.BUFFER);
  }

  public void disableAllTheThings() {
    setBoolean(Key.ENABLE_BACKUP_TO_DRIVE, false);
    setBoolean(Key.ENABLE_PUBLISH_CHARTS_TO_DRIVE, false);
  }

  public void setDefaults(boolean isDebug) {
    setBoolean(Key.CHARTING_REMINDER, !isDebug);
  }

  private void setBoolean(Key key, boolean value) {
    Timber.d("Setting preference %s to %b", key.name(), value);
    preferences.edit()
        .putBoolean(key.name(), value)
        .apply();
  }

  public static class PreferenceSummary {
    private final SharedPreferences mPrefs;

    public PreferenceSummary(SharedPreferences mPrefs) {
      this.mPrefs = mPrefs;
    }

    public boolean backupEnabled() {
      return mPrefs.getBoolean(Key.ENABLE_BACKUP_TO_DRIVE.name(), false);
    }

    public Single<Boolean> backupEnabled(Callable<Single<Boolean>> promptSupplier) {
      return doTheStuff(Key.ENABLE_BACKUP_TO_DRIVE, Key.PROMPTED_FOR_BACKUP, promptSupplier)
          .doOnSuccess(enabled -> {
            mPrefs.edit().putBoolean(Key.ENABLE_BACKUP_TO_DRIVE.name(), enabled).commit();
          });
    }

    public boolean publishEnabled() {
      return mPrefs.getBoolean(Key.ENABLE_PUBLISH_CHARTS_TO_DRIVE.name(), false);
    }

    public boolean autoStickeringEnabled() {
      return mPrefs.getBoolean(Key.AUTO_STICKERING.name(), false);
    }

    public Single<Boolean> publishEnabled(Callable<Single<Boolean>> promptSupplier) {
      return doTheStuff(Key.ENABLE_PUBLISH_CHARTS_TO_DRIVE, Key.PROMPTED_FOR_PUBLISH, promptSupplier)
          .doOnSuccess(enabled -> {
            mPrefs.edit().putBoolean(Key.ENABLE_PUBLISH_CHARTS_TO_DRIVE.name(), enabled).commit();
          });
    }

    public boolean defaultToDemoMode() {
      return mPrefs.getBoolean(Key.DEFAULT_TO_DEMO.name(), false);
    }

    public boolean enableChartingReminder() {
      return mPrefs.getBoolean(Key.CHARTING_REMINDER.name(), false);
    }

    public boolean lhTestMeasurementEnabled() {
      return mPrefs.getBoolean(Key.ENABLE_LH_TEST_MEASUREMENTS.name(), false);
    }

    public boolean clearblueMachineMeasurementEnabled() {
      return mPrefs.getBoolean(Key.ENABLE_CLEARBLUE_MACHINE_MEASUREMENTS.name(), false);
    }

    private Single<Boolean> doTheStuff(Key togglePref, Key promptPref, Callable<Single<Boolean>> promptSupplier) {
      if (mPrefs.getBoolean(togglePref.name(), false)) {
        Timber.v("%s enabled in preferences", togglePref.name());
        return Single.just(true);
      }
      if (mPrefs.getBoolean(promptPref.name(), false)) {
        Timber.v("Not reprompting for %s", togglePref.name());
        return Single.just(false);
      }
      Timber.v("Prompting for %s", togglePref.name());
      return Single.defer(promptSupplier).doOnSuccess(v -> {
        Timber.v("Prompt result: %b", v);
        mPrefs.edit()
            .putBoolean(togglePref.name(), v)
            .putBoolean(promptPref.name(), true)
            .apply();
      });
    }
  }
}

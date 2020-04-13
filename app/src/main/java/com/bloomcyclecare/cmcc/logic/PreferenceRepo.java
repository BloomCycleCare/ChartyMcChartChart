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
    ENABLE_BACKUP_TO_DRIVE,
    ENABLE_PUBLISH_CHARTS_TO_DRIVE,
    DEFAULT_TO_DEMO,
    DEFAULT_TO_GRID
  }

  private  final BehaviorSubject<PreferenceSummary> mSummarySubject = BehaviorSubject.create();
  private final SharedPreferences preferences;

  public static PreferenceRepo create(Context context) {
    return new PreferenceRepo(context);
  }

  private PreferenceRepo(Context context) {
    preferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
    preferences.registerOnSharedPreferenceChangeListener((sharedPreferences, s) -> {
      for (Key key : Key.values()) {
        if (key.name().equals(s)) {
          mSummarySubject.onNext(new PreferenceSummary(sharedPreferences));
        }
      }
    });
    mSummarySubject.onNext(new PreferenceSummary(preferences));
  }

  public PreferenceSummary currentSummary() {
    return mSummarySubject.getValue();
  }

  public Flowable<PreferenceSummary> summaries() {
    return mSummarySubject.toFlowable(BackpressureStrategy.BUFFER);
  }

  public void disableAllTheThings() {
    preferences.edit()
        .putBoolean(Key.ENABLE_BACKUP_TO_DRIVE.name(), false)
        .putBoolean(Key.ENABLE_PUBLISH_CHARTS_TO_DRIVE.name(), false)
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
      return doTheStuff(Key.ENABLE_BACKUP_TO_DRIVE, Key.PROMPTED_FOR_BACKUP, promptSupplier);
    }

    public boolean publishEnabled() {
      return mPrefs.getBoolean(Key.ENABLE_PUBLISH_CHARTS_TO_DRIVE.name(), false);
    }

    public Single<Boolean> publishEnabled(Callable<Single<Boolean>> promptSupplier) {
      return doTheStuff(Key.ENABLE_PUBLISH_CHARTS_TO_DRIVE, Key.PROMPTED_FOR_PUBLISH, promptSupplier);
    }

    public boolean defaultToDemoMode() {
      return mPrefs.getBoolean(Key.DEFAULT_TO_DEMO.name(), false);
    }

    public boolean defaultToGrid() {
      return mPrefs.getBoolean(Key.DEFAULT_TO_GRID.name(), false);
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
            .putBoolean(Key.PROMPTED_FOR_PUBLISH.name(), true)
            .apply();
      });
    }
  }
}

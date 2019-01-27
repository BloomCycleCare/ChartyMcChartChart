package com.roamingroths.cmcc;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

/**
 * Created by parkeroth on 7/1/17.
 */

public class Preferences {

  private boolean prePeakYellowEnabled;
  private boolean postPeakYellowEnabled;
  private boolean specialSamenessYellowEnabled;

  public static Preferences fromShared(Context context) {
    Preferences prefs = new Preferences();
    prefs.update(PreferenceManager.getDefaultSharedPreferences(context));
    return prefs;
  }

  private Preferences() {
    this(false, false, false);
  }

  private Preferences(boolean prePeakYellowEnabled, boolean postPeakYellowEnabled, boolean specialSamenessYellowEnabled) {
    this.prePeakYellowEnabled = prePeakYellowEnabled;
    this.postPeakYellowEnabled = postPeakYellowEnabled;
    this.specialSamenessYellowEnabled = specialSamenessYellowEnabled;
  }

  public void update(SharedPreferences preferences) {
    this.prePeakYellowEnabled = preferences.getBoolean("enable_pre_peak_yellow_stickers", false);
    this.postPeakYellowEnabled = preferences.getBoolean("enable_post_peak_yellow_stickers", false);
    this.specialSamenessYellowEnabled = preferences.getBoolean("special_sameness_yellow_stickers", false);
  }

  public boolean prePeakYellowEnabled() {
    return prePeakYellowEnabled;
  }

  public boolean postPeakYellowEnabled() {
    return postPeakYellowEnabled;
  }

  public boolean specialSamenessYellowEnabled() {
    return specialSamenessYellowEnabled;
  }
}

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

  public static Preferences fromShared(Context context) {
    Preferences prefs = new Preferences();
    prefs.update(PreferenceManager.getDefaultSharedPreferences(context));
    return prefs;
  }

  public Preferences() {
    this(false, false);
  }

  public Preferences(boolean prePeakYellowEnabled, boolean postPeakYellowEnabled) {
    this.prePeakYellowEnabled = prePeakYellowEnabled;
    this.postPeakYellowEnabled = postPeakYellowEnabled;
  }

  public void update(SharedPreferences preferences) {
    this.prePeakYellowEnabled = preferences.getBoolean("enable_pre_peak_yellow_stickers", false);
    this.postPeakYellowEnabled = preferences.getBoolean("enable_post_peak_yellow_stickers", false);
  }

  public boolean prePeakYellowEnabled() {
    return prePeakYellowEnabled;
  }

  public boolean postPeakYellowEnabled() {
    return postPeakYellowEnabled;
  }
}

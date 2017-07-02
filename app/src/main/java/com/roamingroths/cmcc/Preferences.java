package com.roamingroths.cmcc;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

/**
 * Created by parkeroth on 7/1/17.
 */

public class Preferences {

  private final boolean prePeakYellowEnabled;
  private final boolean postPeakYellowEnabled;

  public static Preferences fromShared(Context context) {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    return new Preferences(
        preferences.getBoolean("enable_pre_peak_yellow_stickers", false),
        preferences.getBoolean("enable_post_peak_yellow_stickers", false));
  }

  public Preferences(boolean prePeakYellowEnabled, boolean postPeakYellowEnabled) {
    this.prePeakYellowEnabled = prePeakYellowEnabled;
    this.postPeakYellowEnabled = postPeakYellowEnabled;
  }

  public boolean prePeakYellowEnabled() {
    return prePeakYellowEnabled;
  }

  public boolean postPeakYellowEnabled() {
    return postPeakYellowEnabled;
  }
}

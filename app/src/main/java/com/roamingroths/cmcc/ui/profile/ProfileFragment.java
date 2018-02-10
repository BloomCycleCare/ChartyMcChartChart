package com.roamingroths.cmcc.ui.profile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.DatePickerPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.NumberPickerPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.util.Log;

import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.providers.ProfileProvider;

import io.reactivex.SingleSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import me.philio.preferencecompatextended.PreferenceFragmentCompat;

/**
 * A placeholder fragment containing a simple view.
 */
public class ProfileFragment extends PreferenceFragmentCompat implements
    SharedPreferences.OnSharedPreferenceChangeListener {

  private void setNumPickerSummary(SharedPreferences prefs, NumberPickerPreference pref) {
    int val = prefs.getInt(pref.getKey(), 0);
    String unit = pref.getSubtitleText();
    pref.setSummary(String.format("%d %s", val, unit));
  }

  private void setListSummary(SharedPreferences prefs, ListPreference pref) {
    pref.setSummary(pref.getEntry());
  }

  private void setSwitchSummary(SharedPreferences prefs, SwitchPreference pref) {
    pref.setSummary(String.valueOf(prefs.getBoolean(pref.getKey(), false)));
  }

  private void setDatePickerSummary(SharedPreferences prefs, DatePickerPreference pref) {}

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.pref_profile);

    SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();

    PreferenceScreen prefScreen = getPreferenceScreen();
    int count = prefScreen.getPreferenceCount();
    Log.v("ProfileFragment", "Loading " + count + " preferences");
    for (int i = 0; i < count; i++) {
      updateSummary(prefScreen.getPreference(i), sharedPreferences);
    }
  }

  private void updateSummary(Preference pref, SharedPreferences prefs) {
    if (pref instanceof SwitchPreferenceCompat) {
      setSwitchSummary(prefs, (SwitchPreference) pref);
    } else if (pref instanceof NumberPickerPreference) {
      setNumPickerSummary(prefs, (NumberPickerPreference) pref);
    } else if (pref instanceof ListPreference) {
      setListSummary(prefs, (ListPreference) pref);
    } else if (pref instanceof DatePickerPreference) {
      setDatePickerSummary(prefs, (DatePickerPreference) pref);
    } else {
      pref.setSummary(prefs.getString(pref.getKey(), ""));
    }
  }

  @Override
  public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
    Preference preference = findPreference(key);
    if (null != preference) {
      updateSummary(preference, sharedPreferences);
      MyApplication.profileProvider().subscribe(new Consumer<ProfileProvider>() {
        @Override
        public void accept(ProfileProvider profileProvider) throws Exception {
          profileProvider.maybeUpdateProfile(sharedPreferences, key);
        }
      });
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    // unregister the preference change listener
    getPreferenceScreen().getSharedPreferences()
        .unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onStart() {
    super.onStart();
    // register the preference change listener
    getPreferenceScreen().getSharedPreferences()
        .registerOnSharedPreferenceChangeListener(this);
  }
}

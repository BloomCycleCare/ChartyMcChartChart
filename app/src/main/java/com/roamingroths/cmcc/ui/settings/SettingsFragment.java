package com.roamingroths.cmcc.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.roamingroths.cmcc.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements
    SharedPreferences.OnSharedPreferenceChangeListener {

  private void setPreferenceSummary(Preference preference, Object value) {
    String stringValue = value.toString();
    if (preference instanceof SwitchPreferenceCompat) {
      return;
    }
    if (preference instanceof ListPreference) {
      // For list preferences, look up the correct display value in
      // the preference's 'entries' list (since they have separate labels/values).
      ListPreference listPreference = (ListPreference) preference;
      int prefIndex = listPreference.findIndexOfValue(stringValue);
      if (prefIndex >= 0) {
        preference.setSummary(listPreference.getEntries()[prefIndex]);
      }
    } else {
      // For other preferences, set the summary to the value's simple string representation.
      preference.setSummary(stringValue);
    }
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.pref_settings);

    SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
    PreferenceScreen prefScreen = getPreferenceScreen();
    int count = prefScreen.getPreferenceCount();
    Log.v("ProfileFragment", "Loading " + count + " preferences");
    for (int i = 0; i < count; i++) {
      Preference p = prefScreen.getPreference(i);
      if (p instanceof SwitchPreferenceCompat) {
        setPreferenceSummary(p, sharedPreferences.getBoolean(p.getKey(), false));
        continue;
      }
      if (!(p instanceof CheckBoxPreference)) {
        setPreferenceSummary(p, sharedPreferences.getString(p.getKey(), ""));
        continue;
      }
    }
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    Preference preference = findPreference(key);
    if (null != preference) {
      if (preference instanceof SwitchPreferenceCompat) {
        setPreferenceSummary(preference, sharedPreferences.getBoolean(key, false));
      } else if (preference instanceof MultiSelectListPreference) {
        return;
      } else if (!(preference instanceof CheckBoxPreference)) {
        setPreferenceSummary(preference, sharedPreferences.getString(key, ""));
      }
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

package com.bloomcyclecare.cmcc.ui.profile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.bloomcyclecare.cmcc.R;

import androidx.preference.DatePickerPreference;
import androidx.preference.ListPreference;
import androidx.preference.NumberPickerPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;
import me.philio.preferencecompatextended.PreferenceFragmentCompat;

/**
 * A placeholder fragment containing a simple view.
 */
public class ProfilePageFragment extends PreferenceFragmentCompat implements
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
      // TODO: something with this update
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

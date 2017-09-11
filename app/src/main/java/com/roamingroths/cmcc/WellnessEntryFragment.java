package com.roamingroths.cmcc;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.roamingroths.cmcc.data.WellnessAdapter;

import java.util.Map;
import java.util.Set;

/**
 * Created by parkeroth on 9/11/17.
 */

public class WellnessEntryFragment extends Fragment {

  private RecyclerView mRecyclerView;
  private WellnessAdapter mWellnessAdapter;

  private Map<String, String> getWellnessOptions(SharedPreferences preferences) {
    Set<String> activeOptions =
        preferences.getStringSet("pref_key_wellness_options", ImmutableSet.<String>of());
    Log.v("WellnessEntryFragment", activeOptions.size() + " active options");
    ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.builder();
    String[] values = getActivity().getResources().getStringArray(R.array.pref_wellness_option_values);
    String[] keys = getActivity().getResources().getStringArray(R.array.pref_wellness_option_keys);
    for (int i = 0; i < values.length; i++) {
      Log.v("WellnessEntryFragment", "Key: " + keys[i] + " Value: " + values[i]);
      if (activeOptions.contains(keys[i])) {
        mapBuilder.put(keys[i], values[i]);
      }
    }
    return mapBuilder.build();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.v("WellnessEntryFragment", "onCreateView");

    View view = inflater.inflate(R.layout.fragment_wellness_entry, container, false);
    mWellnessAdapter = new WellnessAdapter(getActivity());

    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    mWellnessAdapter.updateData(getWellnessOptions(preferences));
    preferences.registerOnSharedPreferenceChangeListener(
        new SharedPreferences.OnSharedPreferenceChangeListener() {
          @Override
          public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.v("WellnessEntryFragment", "onSharedPreferenceChanged: " + key);
            if (key.equals("pref_key_wellness_options")) {
              mWellnessAdapter.updateData(getWellnessOptions(sharedPreferences));
            }
          }
        }
    );

    mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerview_wellness_entry);
    mRecyclerView.setLayoutManager(
        new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    mRecyclerView.setHasFixedSize(false);
    mRecyclerView.setAdapter(mWellnessAdapter);

    return view;
  }

  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    Log.v("WellnessEntryFragment", "Hint visible: " + isVisibleToUser);
    if (isVisibleToUser) {
      hideKeyboard();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.v("WellnessEntryFragment", "onCreateView");
    hideKeyboard();
  }

  @Override
  public void onPause() {
    super.onPause();
  }

  private void hideKeyboard() {
    InputMethodManager imm =
        (InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(getView().getRootView().getWindowToken(), 0);
  }
}

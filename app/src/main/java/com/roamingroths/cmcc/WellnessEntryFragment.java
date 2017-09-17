package com.roamingroths.cmcc;

import android.content.Context;
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

import com.google.common.collect.ImmutableSet;
import com.roamingroths.cmcc.utils.MultiSelectPrefAdapter;

/**
 * Created by parkeroth on 9/11/17.
 */

public class WellnessEntryFragment extends Fragment {

  private RecyclerView mRecyclerView;
  private MultiSelectPrefAdapter mAdapter;
  private SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.v("WellnessEntryFragment", "onCreateView");

    View view = inflater.inflate(R.layout.fragment_wellness_entry, container, false);
    String[] values = getActivity().getResources().getStringArray(R.array.pref_wellness_option_values);
    String[] keys = getActivity().getResources().getStringArray(R.array.pref_wellness_option_keys);
    mAdapter = new MultiSelectPrefAdapter(
        getContext(),
        R.layout.wellness_list_item,
        R.id.tv_wellness_item,
        R.id.switch_wellness_item, values, keys, savedInstanceState);

    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    mAdapter.updateActiveItems(
        preferences.getStringSet("pref_key_wellness_options", ImmutableSet.<String>of()));
    mPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
      @Override
      public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.v("WellnessEntryFragment", "onSharedPreferenceChanged: " + key);
        if (key.equals("pref_key_wellness_options")) {
          mAdapter.updateActiveItems(
              sharedPreferences.getStringSet("pref_key_wellness_options", ImmutableSet.<String>of()));
        }
      }
    };
    preferences.registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);

    mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerview_wellness_entry);
    mRecyclerView.setLayoutManager(
        new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    mRecyclerView.setHasFixedSize(false);
    mRecyclerView.setAdapter(mAdapter);

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

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mAdapter.fillBundle(outState);
  }

  private void hideKeyboard() {
    Context context = getContext();
    if (context != null) {
      InputMethodManager imm =
          (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(getView().getRootView().getWindowToken(), 0);
    }
  }
}

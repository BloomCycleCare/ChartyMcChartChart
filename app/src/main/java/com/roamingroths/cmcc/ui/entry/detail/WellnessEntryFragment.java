package com.roamingroths.cmcc.ui.entry.detail;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.logic.chart.WellnessEntry;
import com.roamingroths.cmcc.utils.MultiSelectPrefAdapter;

import java.util.Map;

/**
 * Created by parkeroth on 9/11/17.
 */

public class WellnessEntryFragment extends EntryFragment<WellnessEntry> {

  private RecyclerView mRecyclerView;
  private MultiSelectPrefAdapter mAdapter;
  private SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener;

  public WellnessEntryFragment() {
    super(WellnessEntry.class, "WellnessEntryFragment", R.layout.fragment_wellness_entry);
  }

  @Override
  void duringCreateView(View view, Bundle args, Bundle savedInstanceState) {
    String[] values = getActivity().getResources().getStringArray(R.array.pref_wellness_option_values);
    String[] keys = getActivity().getResources().getStringArray(R.array.pref_wellness_option_keys);
    mAdapter = new MultiSelectPrefAdapter(
        getContext(),
        R.layout.list_item_wellness_entry,
        R.id.tv_wellness_item,
        R.id.switch_wellness_item, values, keys, savedInstanceState);

    PreferenceManager.setDefaultValues(getActivity(), R.xml.pref_general, false);
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    mAdapter.updateActiveItems(
        preferences.getStringSet("pref_key_wellness_options", ImmutableSet.<String>of()));
    mPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
      @Override
      public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("pref_key_wellness_options")) {
          Log.v("WellnessEntryFragment", "onSharedPreferenceChanged: " + key);
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
  }

  @Override
  WellnessEntry processExistingEntry(WellnessEntry existingEntry) {
    MapDifference<String, Boolean> difference =
        Maps.difference(existingEntry.wellnessItems, mAdapter.getActiveEntries());
    if (difference.areEqual()) {
      return existingEntry;
    }

    for (Map.Entry<String, Boolean> entry : difference.entriesOnlyOnLeft().entrySet()) {
      existingEntry.wellnessItems.remove(entry.getKey());
    }
    existingEntry.wellnessItems.putAll(difference.entriesOnlyOnRight());

    return existingEntry;
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
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mAdapter.fillBundle(outState);
  }

  @Override
  public WellnessEntry getEntryFromUi() {
    if (!mUiActive) {
      return null;
    }
    return new WellnessEntry(getEntryDate(), mAdapter.getActiveEntries(), getCycle().keys.wellnessKey);
  }

  private void hideKeyboard() {
    Context context = getContext();
    if (context != null) {
      InputMethodManager imm =
          (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(getView().getRootView().getWindowToken(), 0);
    }
  }

  @Override
  void updateUiWithEntry(WellnessEntry entry) {
    mAdapter.updateValues(entry.wellnessItems);
  }
}

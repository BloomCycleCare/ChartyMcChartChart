package com.roamingroths.cmcc;

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
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.data.EntryProvider;
import com.roamingroths.cmcc.data.SymptomEntryProvider;
import com.roamingroths.cmcc.logic.SymptomEntry;
import com.roamingroths.cmcc.utils.MultiSelectPrefAdapter;

import java.util.Map;

/**
 * Created by parkeroth on 9/11/17.
 */

public class SymptomEntryFragment extends EntryFragment<SymptomEntry> {

  private RecyclerView mRecyclerView;
  private MultiSelectPrefAdapter mAdapter;
  private SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener;

  public SymptomEntryFragment() {
    super(R.layout.fragment_symptom_entry);
  }

  @Override
  EntryProvider<SymptomEntry> createEntryProvider(FirebaseDatabase db) {
    return SymptomEntryProvider.forDb(db);
  }

  @Override
  void duringCreateView(View view, Bundle args, Bundle savedInstanceState) {
    String[] values = getActivity().getResources().getStringArray(R.array.pref_symptom_option_values);
    String[] keys = getActivity().getResources().getStringArray(R.array.pref_symptom_option_keys);
    mAdapter = new MultiSelectPrefAdapter(
        getContext(),
        R.layout.symptom_list_item,
        R.id.tv_symptom_item,
        R.id.switch_symptom_item, values, keys, savedInstanceState);

    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    mAdapter.updateActiveItems(
        preferences.getStringSet("pref_key_symptom_options", ImmutableSet.<String>of()));
    mPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
      @Override
      public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        Log.v("SymptomEntryFragment", "onSharedPreferenceChanged: " + key);
        if (key.equals("pref_key_symptom_options")) {
          mAdapter.updateActiveItems(
              preferences.getStringSet("pref_key_symptom_options", ImmutableSet.<String>of()));
        }
      }
    };

    mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerview_symptom_entry);
    mRecyclerView.setLayoutManager(
        new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    mRecyclerView.setHasFixedSize(false);
    mRecyclerView.setAdapter(mAdapter);
  }

  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    Log.v("SymptomEntryFragment", "Hint visible: " + isVisibleToUser);
    if (isVisibleToUser) {
      hideKeyboard();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    PreferenceManager.getDefaultSharedPreferences(getContext())
        .registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    Log.v("WellnessEntryFragment", "onCreateView");
    hideKeyboard();
  }

  @Override
  public void onPause() {
    super.onPause();
    PreferenceManager.getDefaultSharedPreferences(getContext())
        .unregisterOnSharedPreferenceChangeListener(mPreferenceChangeListener);
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

  @Override
  void updateUiWithEntry(SymptomEntry entry) {
    mAdapter.updateValues(entry.symptoms);
  }

  @Override
  SymptomEntry getEntryFromUi() throws Exception {
    return new SymptomEntry(getEntryDate(), mAdapter.getActiveEntries(), getCycle().keys.symptomKey);
  }

  @Override
  SymptomEntry processExistingEntry(SymptomEntry existingEntry) {
    MapDifference<String, Boolean> difference =
        Maps.difference(existingEntry.symptoms, mAdapter.getActiveEntries());
    if (difference.areEqual()) {
      return existingEntry;
    }

    for (Map.Entry<String, Boolean> entry : difference.entriesOnlyOnLeft().entrySet()) {
      existingEntry.symptoms.remove(entry.getKey());
    }
    existingEntry.symptoms.putAll(difference.entriesOnlyOnRight());

    return existingEntry;
  }
}

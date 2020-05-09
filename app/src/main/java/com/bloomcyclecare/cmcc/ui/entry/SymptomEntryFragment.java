package com.bloomcyclecare.cmcc.ui.entry;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.utils.MultiSelectPrefAdapter;
import com.bloomcyclecare.cmcc.utils.UiUtil;
import com.google.common.collect.ImmutableSet;
import com.jakewharton.rxbinding2.support.v7.widget.RxRecyclerViewAdapter;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by parkeroth on 9/11/17.
 */

public class SymptomEntryFragment extends Fragment {

  private EntryDetailViewModel mViewModel;

  private MultiSelectPrefAdapter mAdapter;
  private SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    mViewModel = ViewModelProviders.of(getActivity()).get(EntryDetailViewModel.class);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    String[] values = getActivity().getResources().getStringArray(R.array.pref_symptom_option_values);
    String[] keys = getActivity().getResources().getStringArray(R.array.pref_symptom_option_keys);
    mAdapter = new MultiSelectPrefAdapter(
        getContext(),
        R.layout.list_item_symptom_entry,
        R.id.tv_symptom_item,
        R.id.switch_symptom_item, values, keys, savedInstanceState);

    PreferenceManager.setDefaultValues(getActivity(), R.xml.pref_settings, false);
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    mAdapter.updateActiveItems(
        preferences.getStringSet("pref_key_symptom_options", ImmutableSet.<String>of()));
    mPreferenceChangeListener = (sharedPreferences, key) -> {
      if (key.equals("pref_key_symptom_options")) {
        Log.v("SymptomEntryFragment", "onSharedPreferenceChanged: " + key);
        mAdapter.updateActiveItems(
            sharedPreferences.getStringSet("pref_key_symptom_options", ImmutableSet.<String>of()));
      }
    };
    preferences.registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_symptom_entry, container, false);

    RecyclerView mRecyclerView = view.findViewById(R.id.recyclerview_symptom_entry);
    mRecyclerView.setLayoutManager(
        new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
    mRecyclerView.setHasFixedSize(false);
    mRecyclerView.setAdapter(mAdapter);

    RxRecyclerViewAdapter
        .dataChanges(mAdapter)
        .map(MultiSelectPrefAdapter::getData)
        .subscribe(mViewModel.symptomUpdates);

    return view;
  }

  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    Log.v("SymptomEntryFragment", "Hint visible: " + isVisibleToUser);
    View view = getView();
    if (view != null && isVisibleToUser) {
      UiUtil.hideKeyboard(getContext(), view.getRootView());
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    PreferenceManager.getDefaultSharedPreferences(getContext())
        .registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);
    if (getView() != null) {
      UiUtil.hideKeyboard(getContext(), getView().getRootView());
    }
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
}

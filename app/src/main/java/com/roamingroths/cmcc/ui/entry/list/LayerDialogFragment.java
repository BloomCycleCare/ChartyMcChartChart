package com.roamingroths.cmcc.ui.entry.list;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.roamingroths.cmcc.R;

/**
 * Created by parkeroth on 11/19/17.
 */

public class LayerDialogFragment extends DialogFragment {

  private Button mButtonCancel;
  private Button mButtonClear;
  private ViewPager mViewPager;
  private EntryListView mEntryListView;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    mEntryListView = (EntryListView) getActivity();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Dialog dialog = super.onCreateDialog(savedInstanceState);
    return dialog;
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.dialog_layer, container);

    mButtonCancel = view.findViewById(R.id.button_cancel);
    mButtonCancel.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        dismiss();
      }
    });
    mButtonClear = view.findViewById(R.id.button_clear);
    mButtonClear.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        mEntryListView.setOverlay("");
        dismiss();
      }
    });
    mViewPager = view.findViewById(R.id.layer_view_pager);
    mViewPager.setAdapter(new LayerPagerAdapter(getChildFragmentManager()));

    return view;
  }

  private static class LayerPagerAdapter extends FragmentStatePagerAdapter {

    public LayerPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      int keyId = position == 0 ? R.array.pref_wellness_option_keys : R.array.pref_symptom_option_keys;
      int valId = position == 0 ? R.array.pref_wellness_option_values : R.array.pref_symptom_option_values;
      return LayerListFragment.newInstance(keyId, valId);
    }

    @Override
    public int getCount() {
      return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      switch (position) {
        case 0:
          return "Wellness";
        case 1:
          return "Symptom";
      }
      return null;
    }
  }
}

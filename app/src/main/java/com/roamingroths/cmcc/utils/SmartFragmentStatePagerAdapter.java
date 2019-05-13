package com.roamingroths.cmcc.utils;

import android.util.SparseArray;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

/**
 * Created by parkeroth on 11/26/17.
 */

public abstract class SmartFragmentStatePagerAdapter<F extends Fragment> extends FragmentStatePagerAdapter {

  // Sparse array to keep track of registered fragments in memory
  private SparseArray<F> registeredFragments = new SparseArray<>();

  public SmartFragmentStatePagerAdapter(FragmentManager fragmentManager) {
    super(fragmentManager);
  }

  // Register the fragment when the item is instantiated
  @Override
  public Object instantiateItem(ViewGroup container, int position) {
    F fragment = (F) super.instantiateItem(container, position);
    registeredFragments.put(position, fragment);
    return fragment;
  }

  // Unregister when the item is inactive
  @Override
  public void destroyItem(ViewGroup container, int position, Object object) {
    registeredFragments.remove(position);
    super.destroyItem(container, position, object);
  }

  // Returns the fragment for the position (if instantiated)
  public F getRegisteredFragment(int position) {
    return registeredFragments.get(position);
  }
}

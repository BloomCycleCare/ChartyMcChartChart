package com.bloomcyclecare.cmcc.ui.entry;

import android.os.Bundle;
import android.util.Pair;

import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.ui.entry.breastfeeding.BreastfeedingEntryFragment;
import com.bloomcyclecare.cmcc.ui.entry.measurement.MeasurementEntryFragment;
import com.bloomcyclecare.cmcc.ui.entry.observation.ObservationEntryFragment;
import com.google.common.collect.ImmutableList;

import org.parceler.Parcels;

import java.util.function.Supplier;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class EntryDetailPagerAdapter extends FragmentStatePagerAdapter {

  private final CycleRenderer.EntryModificationContext mEntryModificationContext;

  private final ImmutableList<Pair<String, Supplier<Fragment>>> fragmentSuppliers;

  public EntryDetailPagerAdapter(FragmentManager fm, CycleRenderer.EntryModificationContext entryModificationContext, boolean shouldShowMeasurementPage, boolean shouldShowBreastfeedingPage) {
    super(fm);
    mEntryModificationContext = entryModificationContext;

    ImmutableList.Builder<Pair<String, Supplier<Fragment>>> builder = ImmutableList.builder();
    builder.add(Pair.create("Observation", ObservationEntryFragment::new));

    if (shouldShowMeasurementPage) {
      builder.add(Pair.create("Measurements", MeasurementEntryFragment::new));
    }
    if (shouldShowBreastfeedingPage) {
      builder.add(Pair.create("Breastfeeding", BreastfeedingEntryFragment::new));
    }

    // TODO: add Wellness and Symptom

    fragmentSuppliers = builder.build();
  }

  @Override
  public Fragment getItem(int position) {
    Bundle args = new Bundle();
    args.putParcelable(CycleRenderer.EntryModificationContext.class.getCanonicalName(), Parcels.wrap(mEntryModificationContext));

    if (position > getCount() - 1) {
      throw new IllegalArgumentException();
    }

    Fragment fragment = fragmentSuppliers.get(position).second.get();
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public int getCount() {
    return fragmentSuppliers.size();
  }

  @Override
  public CharSequence getPageTitle(int position) {
    if (position > getCount() - 1) {
      throw new IllegalArgumentException();
    }
    return fragmentSuppliers.get(position).first;
  }
}

package com.roamingroths.cmcc.ui.entry.list;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.util.SortedList;
import android.util.Log;
import android.view.ViewGroup;

import com.google.common.base.Preconditions;
import com.google.firebase.auth.FirebaseUser;
import com.roamingroths.cmcc.data.ChartEntryProvider;
import com.roamingroths.cmcc.data.CycleProvider;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.ui.entry.detail.EntrySaveResult;
import com.roamingroths.cmcc.utils.SmartFragmentStatePagerAdapter;

import java.util.Comparator;
import java.util.List;

import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by parkeroth on 11/16/17.
 */

public class EntryListPageAdapter extends SmartFragmentStatePagerAdapter<EntryListFragment> {

  private static boolean DEBUG = true;
  private static String TAG = EntryListPageAdapter.class.getSimpleName();

  private final ChartEntryProvider mChartEntryProvider;
  private final SortedList<Cycle> mCycles;

  public EntryListPageAdapter(FragmentManager fragmentManager, ChartEntryProvider chartEntryProvider) {
    super(fragmentManager);
    mChartEntryProvider = chartEntryProvider;
    mCycles = new SortedList<>(Cycle.class, new SortedList.Callback<Cycle>() {
      @Override
      public int compare(Cycle o1, Cycle o2) {
        return o2.startDate.compareTo(o1.startDate);
      }

      @Override
      public boolean areContentsTheSame(Cycle oldItem, Cycle newItem) {
        return oldItem.equals(newItem);
      }

      @Override
      public boolean areItemsTheSame(Cycle item1, Cycle item2) {
        return item1.id.equals(item2.id);
      }

      @Override
      public void onChanged(int position, int count) {
        notifyDataSetChanged();
      }

      @Override
      public void onInserted(int position, int count) {
        notifyDataSetChanged();
      }

      @Override
      public void onRemoved(int position, int count) {
        notifyDataSetChanged();
      }

      @Override
      public void onMoved(int fromPosition, int toPosition) {
        notifyDataSetChanged();
      }
    });

    /*Cycle cycle = intent.getParcelableExtra(Cycle.class.getName());
    if (DEBUG) Log.v(TAG, "Initial cycle: " + cycle.id);
    ArrayList<ChartEntry> containers = intent.getParcelableArrayListExtra(ChartEntry.class.getName());
    mCycles.add(cycle);
    notifyDataSetChanged();*/
  }

  public void initialize(FirebaseUser user, CycleProvider cycleProvider) {
    cycleProvider.getAllCycles(user)
        .sorted(new Comparator<Cycle>() {
          @Override
          public int compare(Cycle o1, Cycle o2) {
            return o2.startDate.compareTo(o1.startDate);
          }
        })
        .toList()
        .subscribe(new Consumer<List<Cycle>>() {
          @Override
          public void accept(List<Cycle> cycles) throws Exception {
            mCycles.beginBatchedUpdates();
            mCycles.addAll(cycles);
            mCycles.endBatchedUpdates();
          }
        });
  }

  public void shutdown(ViewGroup viewGroup) {
    for (int i = 0; i < mCycles.size(); i++) {
      EntryListFragment fragment = (EntryListFragment) instantiateItem(viewGroup, i);
      fragment.shutdown();
    }

    notifyDataSetChanged();
  }

  public int onResult(EntrySaveResult result) {
    for (Cycle cycle : result.droppedCycles) {
      if (DEBUG) Log.v(TAG, "Dropping cycle: " + cycle);
      mCycles.remove(cycle);
    }
    for (Cycle cycle : result.newCycles) {
      if (DEBUG) Log.v(TAG, "Adding cycle: " + cycle);
      mCycles.add(cycle);
    }
    for (Cycle cycle : result.changedCycles) {
      if (DEBUG) Log.v(TAG, "Updating cycle: " + cycle);
      mCycles.updateItemAt(mCycles.indexOf(cycle), cycle);
    }
    notifyDataSetChanged();
    int index = mCycles.indexOf(result.cycle);
    Preconditions.checkState(index >= 0);
    return index;
  }

  // Returns total number of pages
  @Override
  public int getCount() {
    return mCycles.size();
  }

  // Returns the fragment to display for that page
  @Override
  public Fragment getItem(int position) {
    Cycle cycle = mCycles.get(position);

    int leftPosition = position - 1;
    if (leftPosition >= 0) {
      mChartEntryProvider.fillCache(mCycles.get(leftPosition)).subscribeOn(Schedulers.io()).subscribe();
    }
    int rightPosition = position + 1;
    if (rightPosition < mCycles.size()) {
      mChartEntryProvider.fillCache(mCycles.get(rightPosition)).subscribeOn(Schedulers.io()).subscribe();
    }

    if (DEBUG) Log.v(TAG, "getItem() : " + position + " cycle:" + cycle);

    Bundle args = new Bundle();
    args.putParcelable(Cycle.class.getName(), cycle);

    EntryListFragment fragment = new EntryListFragment();

    EntryListFragment leftNeighbor = getRegisteredFragment(position - 1);
    if (leftNeighbor != null) {
      fragment.setNeighbor(leftNeighbor, EntryListFragment.Neighbor.LEFT);
      leftNeighbor.setNeighbor(fragment, EntryListFragment.Neighbor.RIGHT);
    }
    EntryListFragment rightNeighbor = getRegisteredFragment(position + 1);
    if (rightNeighbor != null) {
      fragment.setNeighbor(rightNeighbor, EntryListFragment.Neighbor.RIGHT);
      rightNeighbor.setNeighbor(fragment, EntryListFragment.Neighbor.LEFT);
    }

    fragment.setArguments(args);
    return fragment;
  }

  // Returns the page title for the top indicator
  @Override
  public CharSequence getPageTitle(int position) {
    return "Tab Title";
  }

  @Override
  public int getItemPosition(Object object) {
    EntryListFragment fragment = (EntryListFragment) object;
    int index = mCycles.indexOf(fragment.getCycle());
    //return index < 0 ? POSITION_NONE : index;
    return POSITION_NONE;
  }
}

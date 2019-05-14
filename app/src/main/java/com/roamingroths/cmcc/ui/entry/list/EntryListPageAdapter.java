package com.roamingroths.cmcc.ui.entry.list;

import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.SortedList;

import com.google.common.base.Preconditions;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.ui.entry.EntrySaveResult;
import com.roamingroths.cmcc.utils.SmartFragmentStatePagerAdapter;

import org.parceler.Parcels;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

/**
 * Created by parkeroth on 11/16/17.
 */

public class EntryListPageAdapter extends SmartFragmentStatePagerAdapter<EntryListFragment> {

  private static boolean DEBUG = true;
  private static String TAG = EntryListPageAdapter.class.getSimpleName();

  private final SortedList<Cycle> mCycles;

  public EntryListPageAdapter(FragmentManager fragmentManager) {
    super(fragmentManager);
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

    /*Cycle cycleToShow = intent.getParcelableExtra(Cycle.class.getName());
    if (DEBUG) Log.v(TAG, "Initial cycleToShow: " + cycleToShow.id);
    ArrayList<ChartEntry> containers = intent.getParcelableArrayListExtra(ChartEntry.class.getName());
    mCycles.add(cycleToShow);
    notifyDataSetChanged();*/
  }

  public Disposable attach(Flowable<List<Cycle>> cycleStream) {
    return cycleStream.observeOn(AndroidSchedulers.mainThread()).subscribe(cycles -> {
      // TODO: make this incremental
      mCycles.beginBatchedUpdates();
      mCycles.clear();
      mCycles.addAll(cycles);
      mCycles.endBatchedUpdates();
    }, Timber::e);
  }

  public int onResult(EntrySaveResult result) {
    for (Cycle cycle : result.droppedCycles) {
      if (DEBUG) Log.v(TAG, "Dropping cycleToShow: " + cycle);
      mCycles.remove(cycle);
    }
    for (Cycle cycle : result.newCycles) {
      if (DEBUG) Log.v(TAG, "Adding cycleToShow: " + cycle);
      mCycles.add(cycle);
    }
    for (Cycle cycle : result.changedCycles) {
      if (DEBUG) Log.v(TAG, "Updating cycleToShow: " + cycle);
      mCycles.updateItemAt(mCycles.indexOf(cycle), cycle);
    }
    notifyDataSetChanged();
    int index = mCycles.indexOf(result.cycleToShow);
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
  public EntryListFragment getItem(int position) {
    Cycle cycle = mCycles.get(position);

    if (DEBUG) Log.v(TAG, "getItem() : " + position + " cycleToShow:" + cycle);

    Bundle args = new Bundle();
    args.putParcelable(Cycle.class.getName(), Parcels.wrap(cycle));
    args.putBoolean(EntryListFragment.IS_LAST_CYCLE, position == mCycles.size() - 1);

    EntryListFragment fragment = new EntryListFragment();
    fragment.setArguments(args);

    maybeUpdateFragments(fragment, position);

    return fragment;
  }

  void onPageActive(int position) {
    EntryListFragment f = getRegisteredFragment(position);
    if (f != null) {
      f.onScrollStateUpdate(f.getScrollState());
    }
  }

  private void maybeUpdateFragments(@Nullable EntryListFragment fragment, int position) {
    if (fragment != null) {
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
    }
  }

  @Override
  @NonNull
  public Object instantiateItem(ViewGroup container, int position) {
    EntryListFragment f = (EntryListFragment) super.instantiateItem(container, position);
    maybeUpdateFragments(f, position);
    return f;
  }

  // Returns the page title for the top indicator
  @Override
  public CharSequence getPageTitle(int position) {
    return "Tab Title";
  }

  /*@Override
  public int getItemPosition(Object object) {
    EntryListFragment fragment = (EntryListFragment) object;
    int index = mCycles.indexOf(fragment.getCycle());
    //return index < 0 ? POSITION_NONE : index;
    return POSITION_NONE;
  }*/

  public Cycle getCycle(int position) {
    return mCycles.get(position);
  }
}

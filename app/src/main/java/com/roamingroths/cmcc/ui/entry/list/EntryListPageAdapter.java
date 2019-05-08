package com.roamingroths.cmcc.ui.entry.list;

import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.SortedList;
import android.util.Log;
import android.view.ViewGroup;

import com.google.common.base.Preconditions;
import com.google.firebase.auth.FirebaseUser;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.providers.ChartEntryProvider;
import com.roamingroths.cmcc.providers.CycleProvider;
import com.roamingroths.cmcc.ui.entry.EntrySaveResult;
import com.roamingroths.cmcc.utils.SmartFragmentStatePagerAdapter;

import java.util.Comparator;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.annotations.Nullable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by parkeroth on 11/16/17.
 */

public class EntryListPageAdapter extends SmartFragmentStatePagerAdapter<EntryListFragment> {

  private static boolean DEBUG = true;
  private static String TAG = EntryListPageAdapter.class.getSimpleName();

  private final ChartEntryProvider mChartEntryProvider;
  private final SortedList<Cycle> mCycles;

  public static Function<ChartEntryProvider, EntryListPageAdapter> create(final FragmentManager fragmentManager) {
    return new Function<ChartEntryProvider, EntryListPageAdapter>() {
      @Override
      public EntryListPageAdapter apply(ChartEntryProvider chartEntryProvider) throws Exception {
        return new EntryListPageAdapter(fragmentManager, chartEntryProvider);
      }
    };
  }

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

    /*Cycle cycleToShow = intent.getParcelableExtra(Cycle.class.getName());
    if (DEBUG) Log.v(TAG, "Initial cycleToShow: " + cycleToShow.id);
    ArrayList<ChartEntry> containers = intent.getParcelableArrayListExtra(ChartEntry.class.getName());
    mCycles.add(cycleToShow);
    notifyDataSetChanged();*/
  }

  public static Function<EntryListPageAdapter, Single<EntryListPageAdapter>> initializeFn(
      final Single<FirebaseUser> user, final Single<CycleProvider> cycleProvider) {
    return new Function<EntryListPageAdapter, Single<EntryListPageAdapter>>() {
      @Override
      public Single<EntryListPageAdapter> apply(EntryListPageAdapter adapter) throws Exception {
        return adapter.initialize(user, cycleProvider).andThen(Single.just(adapter));
      }
    };
  }

  public Completable initialize(Single<FirebaseUser> user, Single<CycleProvider> cycleProvider) {
    Single<List<Cycle>> cycles = Single.merge(Single.zip(user, cycleProvider, new BiFunction<FirebaseUser, CycleProvider, Single<List<Cycle>>>() {
      @Override
      public Single<List<Cycle>> apply(FirebaseUser firebaseUser, CycleProvider cycleProvider) throws Exception {
        return cycleProvider
            .getAllCycles(firebaseUser)
            .sorted(new Comparator<Cycle>() {
              @Override
              public int compare(Cycle o1, Cycle o2) {
                return o2.startDate.compareTo(o1.startDate);
              }
            })
            .toList();
      }
    }));
    return Completable.fromSingle(cycles.doOnSuccess(new Consumer<List<Cycle>>() {
      @Override
      public void accept(List<Cycle> cycles) throws Exception {
        mCycles.beginBatchedUpdates();
        mCycles.addAll(cycles);
        mCycles.endBatchedUpdates();
      }
    }));
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

    int leftPosition = position - 1;
    if (leftPosition >= 0) {
      mChartEntryProvider
          .fillCache(mCycles.get(leftPosition))
          .doOnSubscribe(__ -> { if (DEBUG) Log.v(TAG, "Filling cache left"); })
          .doOnComplete(() -> { if (DEBUG) Log.v(TAG, "Done filling cache left"); })
          .subscribeOn(Schedulers.io())
          .subscribe();
    }
    int rightPosition = position + 1;
    if (rightPosition < mCycles.size()) {
      mChartEntryProvider
          .fillCache(mCycles.get(rightPosition))
          .doOnSubscribe(__ -> { if (DEBUG) Log.v(TAG, "Filling cache right"); })
          .doOnComplete(() -> { if (DEBUG) Log.v(TAG, "Done filling cache right"); })
          .subscribeOn(Schedulers.io())
          .subscribe();
    }

    if (DEBUG) Log.v(TAG, "getItem() : " + position + " cycleToShow:" + cycle);

    Bundle args = new Bundle();
    args.putParcelable(Cycle.class.getName(), cycle);

    EntryListFragment fragment = new EntryListFragment();
    fragment.setArguments(args);

    maybeUpdateFragments(fragment, position);

    return fragment;
  }

  public void onPageActive(int position) {
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

  private void maybeUpdateFragments(int position) {
    maybeUpdateFragments(getRegisteredFragment(position), position);
  }

  @Override
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

  @Override
  public int getItemPosition(Object object) {
    EntryListFragment fragment = (EntryListFragment) object;
    int index = mCycles.indexOf(fragment.getCycle());
    //return index < 0 ? POSITION_NONE : index;
    return POSITION_NONE;
  }

  public Cycle getCycle(int position) {
    return mCycles.get(position);
  }
}

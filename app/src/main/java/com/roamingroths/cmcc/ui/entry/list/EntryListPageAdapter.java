package com.roamingroths.cmcc.ui.entry.list;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;
import android.view.ViewGroup;

import com.google.common.collect.Lists;
import com.google.firebase.auth.FirebaseAuth;
import com.roamingroths.cmcc.data.CycleProvider;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.EntryContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

/**
 * Created by parkeroth on 11/16/17.
 */

public class EntryListPageAdapter extends FragmentStatePagerAdapter {

  private static boolean DEBUG = true;
  private static String TAG = EntryListPageAdapter.class.getSimpleName();

  private final CycleProvider mCycleProvider;
  private final List<Cycle> mCycles;
  private final Map<Cycle, ArrayList<EntryContainer>> mContainers;
  private final CompositeDisposable mDisposables;

  public EntryListPageAdapter(FragmentManager fragmentManager, Intent intent, CycleProvider cycleProvider) {
    super(fragmentManager);
    mDisposables = new CompositeDisposable();
    mCycles = new ArrayList<>();
    mContainers = new HashMap<>();

    Cycle cycle = intent.getParcelableExtra(Cycle.class.getName());
    if (DEBUG) Log.v(TAG, "Initial cycle: " + cycle.id);
    ArrayList<EntryContainer> containers = intent.getParcelableArrayListExtra(EntryContainer.class.getName());
    mContainers.put(cycle, containers);
    mCycles.add(cycle);
    notifyDataSetChanged();

    mCycleProvider = cycleProvider;
    mDisposables.add(mCycleProvider
        .getAllCycles(FirebaseAuth.getInstance().getCurrentUser().getUid())
        .filter(new Predicate<Cycle>() {
          @Override
          public boolean test(Cycle cycle) throws Exception {
            return !cycle.equals(mCycles.get(0));
          }
        })
        .sorted(Cycle.comparator())
        .flatMapCompletable(new Function<Cycle, CompletableSource>() {
          @Override
          public CompletableSource apply(Cycle cycle) throws Exception {
            return Completable.fromObservable(Observable.zip(
                Observable.just(cycle),
                mCycleProvider.getEntryContainers(cycle).toList().toObservable(), new BiFunction<Cycle, List<EntryContainer>, Void>() {
                  @Override
                  public Void apply(Cycle cycle, List<EntryContainer> entryContainers) throws Exception {
                    if (DEBUG) Log.v(TAG, "Adding cycle: " + cycle.id);
                    mContainers.put(cycle, Lists.newArrayList(entryContainers));
                    mCycles.add(cycle);
                    notifyDataSetChanged();
                    return null;
                  }
                }));
          }
        })
        .subscribe(new Action() {
          @Override
          public void run() throws Exception {
          }
        }, new Consumer<Throwable>() {
          @Override
          public void accept(Throwable throwable) throws Exception {

          }
        }));
  }

  public void shutdown() {
    mDisposables.clear();
  }

  public EntryListFragment getFragment(Cycle cycle, ViewGroup viewGroup) {
    int index = mCycles.indexOf(cycle);
    return (EntryListFragment) instantiateItem(viewGroup, index);
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

    Bundle args = new Bundle();
    args.putParcelable(Cycle.class.getName(), cycle);
    args.putParcelableArrayList(EntryContainer.class.getName(), mContainers.get(cycle));

    Fragment fragment = new EntryListFragment();
    fragment.setArguments(args);
    return fragment;
  }

  // Returns the page title for the top indicator
  @Override
  public CharSequence getPageTitle(int position) {
    Cycle cycle = mCycles.get(position);
    return cycle.endDate == null ? "Current Cycle" : position + " Cycle Ago";
  }
}

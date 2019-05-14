package com.roamingroths.cmcc.ui.entry.list;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.Maps;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.data.models.ChartEntry;
import com.roamingroths.cmcc.data.repos.ChartEntryRepo;

import org.parceler.Parcels;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

/**
 * Created by parkeroth on 11/13/17.
 */

public class EntryListFragment extends Fragment implements ChartEntryAdapter.OnClickHandler {

  private static boolean DEBUG = false;
  private static String TAG = EntryListFragment.class.getSimpleName();
  private static int SCROLL_POSITION_SAMPLING_PERIOD_MS = 200;
  public static String IS_LAST_CYCLE = "IS_LAST_CYCLE";

  private final Subject<ScrollState> mScrollState;
  private final CompositeDisposable mDisposables = new CompositeDisposable();

  private RecyclerView mRecyclerView;
  private ProgressBar mProgressView;
  private ChartEntryAdapter mChartEntryAdapter;
  private EntryListView mView;
  private Cycle mCycle;
  private Map<Neighbor, WeakReference<EntryListFragment>> mNeighbors;

  public enum Neighbor {
    LEFT, RIGHT
  }

  public EntryListFragment() {
    if (DEBUG) Log.v(TAG, "Construct");
    mNeighbors = Maps.newConcurrentMap();
    mNeighbors.put(Neighbor.LEFT, new WeakReference<EntryListFragment>(null));
    mNeighbors.put(Neighbor.RIGHT, new WeakReference<EntryListFragment>(null));
    mScrollState = BehaviorSubject.create();
    mDisposables.add(mScrollState
        .sample(SCROLL_POSITION_SAMPLING_PERIOD_MS, TimeUnit.MILLISECONDS)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Consumer<ScrollState>() {
          @Override
          public void accept(ScrollState scrollState) throws Exception {
            if (!getUserVisibleHint()) {
              if (DEBUG) Log.v(TAG, "Not scrolling from " + mCycle.startDateStr);
              return;
            }
            onScrollStateUpdate(scrollState);
          }
        }));
  }

  public void setNeighbor(EntryListFragment fragment, Neighbor neighbor) {
    mNeighbors.put(neighbor, new WeakReference<>(fragment));
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    mView = (ChartEntryListActivity) getActivity();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(Cycle.class.getName(), Parcels.wrap(mCycle));
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle arguments = getArguments();
    mCycle = Parcels.unwrap(arguments.getParcelable(Cycle.class.getName()));

    if (DEBUG) Log.v(TAG, "onCreate() cycleToShow starting:" + mCycle.startDateStr);
  }

  public void setScrollState(ScrollState scrollState) {
    if (DEBUG) Log.v(TAG, "Scroll to: "+ scrollState);
    if (mRecyclerView == null) {
      Log.w(EntryListFragment.class.getSimpleName(), "RecyclerView null!");
      return;
    }
    LinearLayoutManager manager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
    int numDays = mRecyclerView.getAdapter().getItemCount();
    int firstVisibleDay =
        (scrollState.firstVisibleDay <= numDays) ? scrollState.firstVisibleDay : numDays;
    int topIndex = numDays - firstVisibleDay;
    manager.scrollToPositionWithOffset(topIndex, scrollState.offsetPixels);
  }

  public void onScrollStateUpdate(ScrollState state) {
    for (Map.Entry<Neighbor, WeakReference<EntryListFragment>> entry : mNeighbors.entrySet()) {
      EntryListFragment neighbor = entry.getValue().get();
      if (neighbor != null) {
        if (DEBUG) Log.v(TAG, "Scrolling " + entry.getKey().name() + " for " + mCycle.startDateStr);
        neighbor.setScrollState(state);
      }
    }
  }

  public ScrollState getScrollState() {
    if (mRecyclerView == null) {
      return null;
    }
    LinearLayoutManager manager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
    int firstPosition = manager.findFirstCompletelyVisibleItemPosition();
    int firstVisibleDay = mRecyclerView.getAdapter().getItemCount() - firstPosition;
    View view = manager.findViewByPosition(firstPosition);
    int offset = (view != null) ? view.getTop() : 0;
    return new ScrollState(firstVisibleDay, offset);
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_chart_list, container, false);

    mProgressView = view.findViewById(R.id.list_load_progress);
    mProgressView.setVisibility(View.VISIBLE);
    mRecyclerView = view.findViewById(R.id.recyclerview_chart_list);
    mRecyclerView.setVisibility(View.INVISIBLE);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        mScrollState.onNext(getScrollState());
      }
    });

    mChartEntryAdapter = new ChartEntryAdapter(
        getActivity().getApplicationContext(),
        mCycle,
        !getArguments().getBoolean(IS_LAST_CYCLE, false),
        EntryListFragment.this,
        "");
    mDisposables.add(((ChartEntryListActivity) getActivity())
        .layerStream()
        .subscribe(mChartEntryAdapter::updateLayerKey));

    MyApplication myApp = MyApplication.cast(getActivity().getApplication());
    ChartEntryRepo entryRepo = new ChartEntryRepo(myApp.db());

    mDisposables.add(entryRepo
        .getStream(Flowable.just(mCycle))
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(mChartEntryAdapter::initialize, Timber::w));

    mRecyclerView.setAdapter(mChartEntryAdapter);
    mChartEntryAdapter.notifyDataSetChanged();
    mProgressView.setVisibility(View.INVISIBLE);
    mRecyclerView.setVisibility(View.VISIBLE);

    mDisposables.add(entryRepo
        .getStream(Flowable.just(mCycle))
        .firstOrError()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.computation())
        .subscribe(chartEntries -> {
          for (WeakReference<EntryListFragment> ref : mNeighbors.values()) {
            EntryListFragment neighbor = ref.get();
            if (neighbor != null) {
              if (DEBUG) Log.v(TAG, "Cycle starting " + mCycle.startDateStr + " has neighbor starting " + neighbor.mCycle.startDateStr);
              ScrollState scrollState = neighbor.getScrollState();
              if (scrollState != null) {
                if (DEBUG) Log.v(TAG, "ScrollState: " + scrollState);
                setScrollState(scrollState);
              }
            }
          }
          if (getUserVisibleHint()) {
            setScrollState(new ScrollState(chartEntries.size(), 0));
          }
        }));

    return view;
  }

  @Override
  public void onClick(ChartEntry container, int index) {
    startActivityForResult(mChartEntryAdapter.getIntentForModification(container, index), 0);
  }

  public void shutdown() {
    if (mChartEntryAdapter != null) {
      // mChartEntryAdapter.shutdown();
    }
  }

  @Override
  public void onDestroy() {
    mChartEntryAdapter.shutdown();

    mDisposables.dispose();

    super.onDestroy();
  }

  public Cycle getCycle() {
    return mCycle;
  }

  public static class ScrollState {
    final int firstVisibleDay;
    final int offsetPixels;

    ScrollState(int firstVisibleDay, int offsetPixels) {
      this.firstVisibleDay = firstVisibleDay;
      this.offsetPixels = offsetPixels;
    }

    @Override
    public String toString() {
      return ("First visible: " + firstVisibleDay + " Offset: " + offsetPixels);
    }
  }
}

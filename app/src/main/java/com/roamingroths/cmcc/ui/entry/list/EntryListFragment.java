package com.roamingroths.cmcc.ui.entry.list;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.collect.Maps;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.ChartEntryProvider;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by parkeroth on 11/13/17.
 */

public class EntryListFragment extends Fragment implements ChartEntryAdapter.OnClickHandler {

  private static boolean DEBUG = false;
  private static String TAG = EntryListFragment.class.getSimpleName();
  private static int SCROLL_POSITION_SAMPLING_PERIOD_MS = 200;

  private final Subject<ScrollState> mScrollState;

  private RecyclerView mRecyclerView;
  private ChartEntryAdapter mChartEntryAdapter;
  private EntryListView mView;
  private FirebaseDatabase mDb;
  private ArrayList<ChartEntry> mChartEntries;
  private Cycle mCycle;
  private Map<Neighbor, WeakReference<EntryListFragment>> mNeighbors;
  private ChartEntryProvider mChartEntryProvider;

  public enum Neighbor {
    LEFT, RIGHT;
  }

  public EntryListFragment() {
    mChartEntryProvider = MyApplication.getProviders().forChartEntry();
    mNeighbors = Maps.newConcurrentMap();
    mNeighbors.put(Neighbor.LEFT, new WeakReference<EntryListFragment>(null));
    mNeighbors.put(Neighbor.RIGHT, new WeakReference<EntryListFragment>(null));
    mScrollState = BehaviorSubject.create();
    mScrollState
        .sample(SCROLL_POSITION_SAMPLING_PERIOD_MS, TimeUnit.MILLISECONDS)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Consumer<ScrollState>() {
          @Override
          public void accept(ScrollState scrollState) throws Exception {
            if (!getUserVisibleHint()) {
              if (DEBUG) Log.v(TAG, "Not scrolling from " + mCycle.startDateStr);
              return;
            }
            for (Map.Entry<Neighbor, WeakReference<EntryListFragment>> entry : mNeighbors.entrySet()) {
              EntryListFragment neighbor = entry.getValue().get();
              if (neighbor != null) {
                if (DEBUG) Log.v(TAG, "Scrolling " + entry.getKey().name() + " for " + mCycle.startDateStr);
                neighbor.setScrollState(scrollState);
              }
            }
          }
        });
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
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mDb = FirebaseDatabase.getInstance();

    Bundle arguments = getArguments();
    mChartEntries = arguments.getParcelableArrayList(ChartEntry.class.getName());
    mCycle = arguments.getParcelable(Cycle.class.getName());

    if (DEBUG) Log.v(TAG, "onCreate() cycleToShow starting:" + mCycle.startDateStr);

    mChartEntryAdapter = new ChartEntryAdapter(
        getActivity().getApplicationContext(),
        mCycle,
        this,
        new ChartEntryProvider(mDb, MyApplication.getCryptoUtil()),
        "");
    if (mChartEntries != null) {
      mChartEntryAdapter.initialize(mChartEntries);
    }

    ((ChartEntryListActivity) getActivity()).layerStream().subscribe(new Consumer<String>() {
      @Override
      public void accept(String s) throws Exception {
        mChartEntryAdapter.updateLayerKey(s);
      }
    });
  }

  public void setScrollState(ScrollState scrollState) {
    if (mRecyclerView == null) {
      Log.w(EntryListFragment.class.getSimpleName(), "RecyclerView null!");
      return;
    }
    LinearLayoutManager manager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
    int numDays = mRecyclerView.getAdapter().getItemCount();
    int firstVisibleDay =
        (scrollState.firstVisibleDay < numDays) ? scrollState.firstVisibleDay : numDays - 1;
    int topIndex = numDays - firstVisibleDay;
    manager.scrollToPositionWithOffset(topIndex, scrollState.offsetPixels);
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
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_chart_list, container, false);

    mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerview_chart_list);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

    mRecyclerView.setAdapter(mChartEntryAdapter);
    mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        mScrollState.onNext(getScrollState());
      }
    });
    mChartEntryAdapter.notifyDataSetChanged();

    mChartEntryProvider
        .getEntries(mCycle)
        .subscribeOn(Schedulers.io())
        .toList()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Consumer<List<ChartEntry>>() {
          @Override
          public void accept(List<ChartEntry> chartEntries) throws Exception {
            mChartEntryAdapter.initialize(chartEntries);
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
          }
        });

    if (mChartEntries != null && !mChartEntries.isEmpty()) {
      mView.showList();
    }

    if (DEBUG) Log.v(TAG, "onCreateView: done for " + mCycle.startDateStr);

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    mChartEntryAdapter.start();
  }

  @Override
  public void onPause() {
    super.onPause();
    mChartEntryAdapter.shutdown();
  }

  @Override
  public void onClick(ChartEntry container, int index) {
    startActivityForResult(mChartEntryAdapter.getIntentForModification(container, index), 0);
  }

  public void shutdown() {
    if (mChartEntryAdapter != null) {
      mChartEntryAdapter.shutdown();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mChartEntryAdapter.shutdown();
  }

  public Cycle getCycle() {
    return mCycle;
  }

  public static class ScrollState {
    public final int firstVisibleDay;
    public final int offsetPixels;

    public ScrollState(int firstVisibleDay, int offsetPixels) {
      this.firstVisibleDay = firstVisibleDay;
      this.offsetPixels = offsetPixels;
    }

    @Override
    public String toString() {
      return new StringBuilder().append("First visible: " + firstVisibleDay + " Offset: " + offsetPixels).toString();
    }
  }
}

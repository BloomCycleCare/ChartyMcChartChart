package com.bloomcyclecare.cmcc.ui.entry.list.vertical;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.google.common.collect.Maps;

import org.parceler.Parcels;

import java.lang.ref.WeakReference;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

/**
 * Created by parkeroth on 11/13/17.
 */

public class EntryListFragment extends Fragment {

  public enum Extras {
    CURRENT_CYCLE,
    IS_LAST_CYCLE,
    VIEW_MODE
  }

  private final CompositeDisposable mDisposables = new CompositeDisposable();

  private RecyclerView mRecyclerView;
  private ProgressBar mProgressView;
  private ChartEntryAdapter mChartEntryAdapter;
  private Map<Neighbor, WeakReference<EntryListFragment>> mNeighbors;
  private FragmentViewModel mViewModel;

  public enum Neighbor {
    LEFT, RIGHT
  }

  public EntryListFragment() {
    mNeighbors = Maps.newConcurrentMap();
    mNeighbors.put(Neighbor.LEFT, new WeakReference<>(null));
    mNeighbors.put(Neighbor.RIGHT, new WeakReference<>(null));
  }

  public void setNeighbor(EntryListFragment fragment, Neighbor neighbor) {
    mNeighbors.put(neighbor, new WeakReference<>(fragment));
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle arguments = getArguments();
    Cycle cycle = Parcels.unwrap(arguments.getParcelable(Extras.CURRENT_CYCLE.name()));

    ViewMode viewMode = ViewMode.values()[arguments.getInt(Extras.VIEW_MODE.name())];
    FragmentViewModel.Factory factory = new FragmentViewModel.Factory(
        getActivity().getApplication(), viewMode, cycle);
    mViewModel = ViewModelProviders.of(this, factory).get(FragmentViewModel.class);

    Timber.v("Created Fragment for cycle starting %s", cycle.startDateStr);
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_chart_list, container, false);

    mRecyclerView = view.findViewById(R.id.recyclerview_chart_list);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        mViewModel.updateScrollState(getScrollState());
      }
    });

    mChartEntryAdapter = new ChartEntryAdapter(
        getActivity(),
        !getArguments().getBoolean(Extras.IS_LAST_CYCLE.name(), false),
        "",
        this::navigateToDetailActivity);
    // TODO: enable layer stream
    /*mDisposables.add(((ChartEntryListActivity) getActivity())
        .layerStream()
        .subscribe(mChartEntryAdapter::updateLayerKey));*/

    mRecyclerView.setAdapter(mChartEntryAdapter);
    mChartEntryAdapter.notifyDataSetChanged();

    mViewModel.updateScrollState(getScrollState());
    mViewModel.viewState().observe(getViewLifecycleOwner(), this::render);

    return view;
  }

  private void render(FragmentViewModel.ViewState viewState) {
    mChartEntryAdapter.update(viewState.renderableCycle, viewState.viewMode);
    if (getUserVisibleHint()) {
      onScrollStateUpdate(viewState.scrollState);
    }
  }

  public FragmentViewModel.ScrollState getScrollState() {
    if (mRecyclerView == null) {
      return null;
    }
    LinearLayoutManager manager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
    int firstPosition = manager.findFirstCompletelyVisibleItemPosition();
    int firstVisibleDay = mRecyclerView.getAdapter().getItemCount() - firstPosition;
    View view = manager.findViewByPosition(firstPosition);
    int offset = (view != null) ? view.getTop() : 0;
    return FragmentViewModel.ScrollState.create(firstVisibleDay, offset);
  }

  public void onScrollStateUpdate(FragmentViewModel.ScrollState state) {
    for (Map.Entry<Neighbor, WeakReference<EntryListFragment>> entry : mNeighbors.entrySet()) {
      EntryListFragment neighbor = entry.getValue().get();
      if (neighbor != null) {
        neighbor.setScrollState(state);
      }
    }
  }

  private void setScrollState(FragmentViewModel.ScrollState scrollState) {
    if (mRecyclerView == null) {
      Timber.w("RecyclerView null!");
      return;
    }
    LinearLayoutManager manager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
    int numDays = mRecyclerView.getAdapter().getItemCount();
    int firstVisibleDay = Math.min(scrollState.firstVisibleDay(), numDays);
    int topIndex = numDays - firstVisibleDay;
    manager.scrollToPositionWithOffset(topIndex, scrollState.offsetPixels());
  }

  private void navigateToDetailActivity(Intent intent) {
    startActivityForResult(intent, 0);
  }

  @Override
  public void onDestroy() {
    mChartEntryAdapter.shutdown();

    mDisposables.dispose();

    super.onDestroy();
  }
}

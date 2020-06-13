package com.bloomcyclecare.cmcc.ui.cycle.vertical;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bloomcyclecare.cmcc.R;
import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.ui.cycle.StickerDialogFragment;
import com.bloomcyclecare.cmcc.ui.entry.EntryDetailActivity;
import com.google.common.collect.Maps;

import org.parceler.Parcels;

import java.lang.ref.WeakReference;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
  private EntryListAdapter mEntryListAdapter;
  private Map<Neighbor, WeakReference<EntryListFragment>> mNeighbors;
  private EntryListViewModel mViewModel;

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
    EntryListViewModel.Factory factory = new EntryListViewModel.Factory(
        getActivity().getApplication(), viewMode, cycle);
    mViewModel = ViewModelProviders.of(this, factory).get(EntryListViewModel.class);

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

    mEntryListAdapter = new EntryListAdapter(
        getActivity(),
        "",
        re -> {
          if (!re.canNavigateToDetailActivity()) {
            Timber.d("Not navigating to detail activity");
            return;
          }
          navigateToDetailActivity(re.entryModificationContext());
        },
        re -> {
          if (!re.canPromptForStickerSelection()) {
            Timber.d("Not prompting for sticker selection");
            return;
          }
          if (!re.hasObservation()) {
            new AlertDialog.Builder(requireContext())
                .setTitle("Observation Required")
                .setMessage("An observation is required before selecting a sticker. Would you like to input an observation now?")
                .setPositiveButton("Yes", (dialog, which) -> {
                  navigateToDetailActivity(re.entryModificationContext());
                  dialog.dismiss();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
            return;
          }
          StickerDialogFragment fragment = new StickerDialogFragment(result -> {
            Timber.i("Selection: %s", result.selection);
            mDisposables.add(mViewModel.updateStickerSelection(re.entryDate(), result.selection).subscribe(
                () -> Timber.d("Done updating selection"),
                t -> Timber.e(t, "Error updating selection")));
            if (!result.ok()) {
              Toast.makeText(requireContext(), "Incorrect selection", Toast.LENGTH_SHORT).show();
            }
          });
          if (!re.expectedStickerSelection().isPresent()) {
            Timber.w("Expected to have a sticker selection");
            return;
          }
          fragment.setArguments(StickerDialogFragment.fillArgs(
              new Bundle(), re.expectedStickerSelection().get(), re.manualStickerSelection(), mViewModel.viewMode()));
          fragment.show(getChildFragmentManager(), "tag");
        },
        MyApplication.cast(requireActivity().getApplication()).showcaseManager());
    // TODO: enable layer stream
    /*mDisposables.add(((ChartEntryListActivity) getActivity())
        .layerStream()
        .subscribe(mChartEntryAdapter::updateLayerKey));*/

    mRecyclerView.setAdapter(mEntryListAdapter);
    mEntryListAdapter.notifyDataSetChanged();

    mViewModel.updateScrollState(getScrollState());
    mViewModel.viewState().observe(getViewLifecycleOwner(), this::render);

    return view;
  }

  private void render(EntryListViewModel.ViewState viewState) {
    Timber.v("Rendering ViewState for cycle %s", viewState.cycle.startDate);
    mEntryListAdapter.update(viewState.cycle, viewState.renderedEntries, viewState.viewMode, viewState.entryShowcaseDate, viewState.showcaseStickerSelection);
    if (getUserVisibleHint()) {
      onScrollStateUpdate(viewState.scrollState);
    }
  }

  public EntryListViewModel.ScrollState getScrollState() {
    if (mRecyclerView == null) {
      return null;
    }
    LinearLayoutManager manager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
    int firstPosition = manager.findFirstCompletelyVisibleItemPosition();
    int firstVisibleDay = mRecyclerView.getAdapter().getItemCount() - firstPosition;
    View view = manager.findViewByPosition(firstPosition);
    int offset = (view != null) ? view.getTop() : 0;
    return EntryListViewModel.ScrollState.create(firstVisibleDay, offset);
  }

  public void onScrollStateUpdate(EntryListViewModel.ScrollState state) {
    for (Map.Entry<Neighbor, WeakReference<EntryListFragment>> entry : mNeighbors.entrySet()) {
      EntryListFragment neighbor = entry.getValue().get();
      if (neighbor != null) {
        neighbor.setScrollState(state);
      }
    }
  }

  private void setScrollState(EntryListViewModel.ScrollState scrollState) {
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

  @Deprecated
  private void navigateToDetailActivity(Intent intent) {
    startActivityForResult(intent, 0);
  }

  private void navigateToDetailActivity(CycleRenderer.EntryModificationContext entryModificationContext) {
    Intent intent = EntryDetailActivity.createIntent(requireContext(), entryModificationContext);
    navigateToDetailActivity(intent);
  }

  @Override
  public void onDestroy() {
    mEntryListAdapter.shutdown();

    mDisposables.dispose();

    super.onDestroy();
  }
}

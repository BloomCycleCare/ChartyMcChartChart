package com.roamingroths.cmcc.ui.entry.list;

import android.app.Application;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;

import com.roamingroths.cmcc.Preferences;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.data.models.ChartEntry;
import com.roamingroths.cmcc.data.models.ChartEntryList;
import com.roamingroths.cmcc.data.repos.ChartEntryRepo;
import com.roamingroths.cmcc.data.repos.CycleRepo;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.BehaviorSubject;

public class EntryListViewModel extends AndroidViewModel {

  private static int SCROLL_POSITION_SAMPLING_PERIOD_MS = 200;
  private static int NUM_NEIGHBORS_TO_PRELOAD = 1;

  private final Preferences mPreferences;
  private final CycleRepo mCycleRepo;
  private final ChartEntryRepo mEntryRepo;

  private final BehaviorSubject<Integer> mCurrentIndex = BehaviorSubject.create();
  private final BehaviorSubject<ScrollState> mScrollState = BehaviorSubject.create();
  private final LiveData<ViewState> mViewState;

  public EntryListViewModel(@NonNull Application application) {
    super(application);

    mPreferences = Preferences.fromShared(application);
    mCycleRepo = new CycleRepo(MyApplication.cast(application).db());
    mEntryRepo = new ChartEntryRepo(MyApplication.cast(application).db());

    mCurrentIndex.onNext(0);  // Start the view at the most recent cycle

    mViewState = LiveDataReactiveStreams.fromPublisher(
        Flowable.combineLatest(
            mCycleRepo.getStream().distinctUntilChanged(),
            mCurrentIndex.toFlowable(BackpressureStrategy.DROP),
            this::createViewState));
  }

  public LiveData<ViewState> viewState() {
    return mViewState;
  }

  public void shiftCycleNext() {
    mCurrentIndex.onNext(mCurrentIndex.getValue() - 1);
  }

  public void shiftCyclePrevious() {
    mCurrentIndex.onNext(mCurrentIndex.getValue() + 1);
  }

  public void updateScrollState(Fragment activeFragment, int firstVisibleDay, int offsetPixels) {
    mScrollState.onNext(new ScrollState(activeFragment, firstVisibleDay, offsetPixels));
  }

  public LiveData<ScrollState> scrollState() {
    return LiveDataReactiveStreams.fromPublisher(mScrollState
        .toFlowable(BackpressureStrategy.DROP)
        .sample(SCROLL_POSITION_SAMPLING_PERIOD_MS, TimeUnit.MILLISECONDS));
  }

  public static class ViewState {

    public final int currentCycleIndex;
    public final SparseArray<Flowable<ChartEntryList>> entryLists;

    public ViewState(int currentCycleIndex, SparseArray<Flowable<ChartEntryList>> entryLists) {
      this.currentCycleIndex = currentCycleIndex;
      this.entryLists = entryLists;
    }
  }

  private ViewState createViewState(List<Cycle> cycleList, int currentIndex) {
    SparseArray<Flowable<ChartEntryList>> cycleArray = new SparseArray<>(NUM_NEIGHBORS_TO_PRELOAD);
    ViewState viewState = new ViewState(currentIndex, cycleArray);

    if (cycleList.isEmpty()) {
      return viewState;
    }
    for (int i=currentIndex-NUM_NEIGHBORS_TO_PRELOAD; i<=currentIndex+NUM_NEIGHBORS_TO_PRELOAD; i++) {
      int adjustedIndex = i + NUM_NEIGHBORS_TO_PRELOAD;
      if (adjustedIndex < 0 || adjustedIndex >= cycleList.size()) {
        // No cycle available for this position
        continue;
      }
      cycleArray.put(adjustedIndex, entryListForCycle(cycleList.get(adjustedIndex)));
    }
    return viewState;
  }

  private Flowable<ChartEntryList> entryListForCycle(Cycle cycle) {
    return mEntryRepo
        .getStream(Flowable.just(cycle))
        .distinctUntilChanged()
        .map(entries -> {
          ChartEntryList entryList = ChartEntryList.builder(cycle, mPreferences).build();
          for (ChartEntry entry : entries) {
            entryList.addEntry(entry);
          }
          return entryList;
        });
  }

  public static class ScrollState {
    final Fragment activeFragment;
    final int firstVisibleDay;
    final int offsetPixels;

    ScrollState(Fragment activeFragment, int firstVisibleDay, int offsetPixels) {
      this.activeFragment = activeFragment;
      this.firstVisibleDay = firstVisibleDay;
      this.offsetPixels = offsetPixels;
    }

    @Override
    public String toString() {
      return ("First visible: " + firstVisibleDay + " Offset: " + offsetPixels);
    }
  }
}

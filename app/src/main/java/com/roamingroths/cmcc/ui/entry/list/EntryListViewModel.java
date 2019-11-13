package com.roamingroths.cmcc.ui.entry.list;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;

import com.google.common.collect.ImmutableList;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.logic.chart.CycleRenderer;
import com.roamingroths.cmcc.utils.RxUtil;

import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class EntryListViewModel extends AndroidViewModel {

  public Subject<Integer> currentPageUpdates = BehaviorSubject.createDefault(0);

  private final Subject<ViewState> mViewStates = BehaviorSubject.create();

  public EntryListViewModel(@NonNull Application application) {
    super(application);

    MyApplication myApp = MyApplication.cast(application);

    Flowable<String> subtitleStream = Flowable.combineLatest(
        currentPageUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        Flowable.merge(Flowable.combineLatest(
            myApp.instructionsRepo()
                .getAll()
                .distinctUntilChanged(),
            myApp.cycleRepo()
                .getStream()
                .distinctUntilChanged(),
            (instructions, cycles) -> Flowable.merge(Flowable
                .fromIterable(cycles)
                .observeOn(Schedulers.computation())
                .parallel()
                .map(cycle -> myApp.entryRepo()
                    .getStreamForCycle(Flowable.just(cycle))
                    .map(entries -> new CycleRenderer(cycle, entries, instructions).render())
                )
                .sequential()
                .toList()
                .toFlowable()
                .map(RxUtil::combineLatest))))
            .distinctUntilChanged(),
        (currentPage, renderableCycles) -> {
          CycleRenderer.CycleStats stats = renderableCycles.get(currentPage).stats;
          if (stats.daysPrePeak == null) {
            return "In prepeak phase";
          }
          String pre = String.format("%d", stats.daysPrePeak);
          String post = stats.daysPostPeak == null ? "n/a" : String.format("%d", stats.daysPostPeak);
          return String.format("Pre: %s Post: %s", pre, post);
        });

    Flowable.combineLatest(
        currentPageUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        subtitleStream.distinctUntilChanged(),
        myApp.cycleRepo().getStream(),
        ViewState::new)
        .toObservable()
        .subscribe(mViewStates);
  }

  LiveData<ViewState> viewStates() {
    return LiveDataReactiveStreams.fromPublisher(mViewStates
        .toFlowable(BackpressureStrategy.DROP)
        .doOnNext(viewState -> Timber.d("Publishing new ViewState")));
  }

  public static class ViewState {
    final String title;
    final String subtitle;
    final boolean showFab;

    final int currentCycleIndex;
    final ImmutableList<Cycle> cycles;

    ViewState(int currentPage, String subtitle, List<Cycle> cycles) {
      this.title = currentPage == 0 ? "Current Cycle" : String.format("%d Cycles Ago", currentPage);
      this.subtitle = subtitle;
      this.showFab = currentPage == cycles.size() - 1;

      this.currentCycleIndex = currentPage;
      this.cycles = ImmutableList.copyOf(cycles);
    }
  }
}

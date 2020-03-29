package com.bloomcyclecare.cmcc.ui.entry.list;

import android.app.Application;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.entities.Pregnancy;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.utils.RxUtil;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.util.Supplier;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Single;
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

    Flowable<List<CycleRenderer.CycleStats>> statsStream = Flowable.merge(Flowable.combineLatest(
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
            .map(cycle -> Flowable.combineLatest(
                myApp.entryRepo().getStreamForCycle(Flowable.just(cycle)),
                myApp.cycleRepo().getPreviousCycle(cycle)
                    .map(Optional::of).defaultIfEmpty(Optional.absent())
                    .toFlowable(),
                (entries, previousCycle) -> new CycleRenderer(cycle, previousCycle, entries, instructions).render())
                .map(renderableCycle -> renderableCycle.stats)
            )
            .sequential()
            .toList()
            .toFlowable()
            .map(RxUtil::combineLatest))))
        .distinctUntilChanged();

    Flowable<String> subtitleStream = Flowable.combineLatest(
        currentPageUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        statsStream,
        currentPageUpdates.flatMap(index -> myApp.cycleRepo().getStream()
            .firstOrError()
            .map(cycles -> cycles.get(index))
            .map(cycle -> Optional.fromNullable(cycle.pregnancyId))
            .flatMap(id -> !id.isPresent()
                ? Single.just(Optional.<Pregnancy>absent())
                : myApp.pregnancyRepo().get(id.get()).map(Optional::of).toSingle(Optional.absent()))
            .toObservable())
        .toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        (currentPage, stats, pregnancy) -> subtitle(stats, currentPage, LocalDate::now, pregnancy));

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

  public static String subtitle(List<CycleRenderer.CycleStats> statsList, int index, Supplier<LocalDate> todaySupplier, Optional<Pregnancy> pregnancy) {
    if (statsList.isEmpty()) {
      return "No data...";
    }
    if (index >= statsList.size()) {
      return "Invalid index!";
    }
    LocalDate today = todaySupplier.get();
    CycleRenderer.CycleStats currentStats = statsList.get(index);
    if (pregnancy.isPresent()) {
      if (pregnancy.get().dueDate != null) {
        if (pregnancy.get().deliveryDate != null) {
          int daysAfterDueDate = Days.daysBetween(pregnancy.get().dueDate, pregnancy.get().deliveryDate).getDays();
          return String.format("Delivery-Due: %d", daysAfterDueDate);
        }
        int daysToDueDate = Days.daysBetween(today, pregnancy.get().dueDate).getDays();
        int weeksToDueDate = daysToDueDate / 7;
        return String.format("Week %d of pregnancy", 40-weeksToDueDate);
      }
      return "Pregnant, due date TBD";
    }
    if (currentStats.daysPrePeak == null) {
      return "In prepeak phase";
    }
    if (currentStats.daysPostPeak != null) {
      return String.format(Locale.getDefault(), "Pre: %d Post: %d", currentStats.daysPrePeak, currentStats.daysPostPeak);
    }
    SummaryStatistics summaryStats = new SummaryStatistics();
    for (int i=index+1; i<statsList.size() && i <= 3; i++) {
      CycleRenderer.CycleStats s = statsList.get(i);
      if (s.daysPostPeak == null) {
        continue;
      }
      summaryStats.addValue(statsList.get(i).daysPostPeak); }
    LocalDate peakDay = currentStats.cycleStartDate.plusDays(currentStats.daysPrePeak);
    if (summaryStats.getN() < 3 || summaryStats.getStandardDeviation() > 1.0) {
      int daysPostPeak = Days.daysBetween(peakDay, today).getDays();
      return String.format(Locale.getDefault(), "%d days postpeak", daysPostPeak);
    }
    LocalDate probableEndDate = peakDay.plusDays((int) Math.round(summaryStats.getMean()));
    int daysUntilPotentialEnd = Days.daysBetween(today, probableEndDate).getDays();
    double ci95 = 1.960 * summaryStats.getStandardDeviation() / Math.sqrt(summaryStats.getN());
    return String.format(Locale.getDefault(), "Potential end: %d±%.1f days", daysUntilPotentialEnd, ci95);
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

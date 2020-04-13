package com.bloomcyclecare.cmcc.ui.entry.list;

import android.app.Application;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.entities.Pregnancy;
import com.bloomcyclecare.cmcc.data.repos.cycle.ROCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.entry.ROChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.instructions.ROInstructionsRepo;
import com.bloomcyclecare.cmcc.data.repos.pregnancy.ROPregnancyRepo;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.utils.RxUtil;
import com.google.common.collect.ImmutableList;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Supplier;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class EntryListViewModel extends AndroidViewModel {

  public Subject<Integer> currentPageUpdates = BehaviorSubject.createDefault(0);

  private final MyApplication mApplication;
  private final Subject<ViewMode> mViewMode = BehaviorSubject.create();
  private final Subject<Optional<LayoutMode>> mTargetLayoutMode = BehaviorSubject.create();
  private final Subject<LayoutMode> mCurrentLayoutMode = BehaviorSubject.create();
  private final Subject<ViewState> mViewStates = BehaviorSubject.create();
  private final Subject<Flowable<ViewState>> mViewStateStream = BehaviorSubject.create();

  private EntryListViewModel(@NonNull Application application, ViewMode viewMode) {
    super(application);
    mApplication = MyApplication.cast(application);
    mViewMode.onNext(viewMode);
    mViewMode.map(this::viewStateStream).subscribe(mViewStateStream);
    mViewStateStream.switchMap(Flowable::toObservable).subscribe(mViewStates);

    LayoutMode initialLayoutMode = mApplication.preferenceRepo().currentSummary().defaultToGrid()
        ? LayoutMode.GRID : LayoutMode.LIST;
    mTargetLayoutMode.onNext(Optional.of(initialLayoutMode));
  }

  private Flowable<ViewState> viewStateStream(ViewMode viewMode) {
    ROInstructionsRepo instructionsRepo = mApplication.instructionsRepo(viewMode);
    ROCycleRepo cycleRepo = mApplication.cycleRepo(viewMode);
    ROChartEntryRepo entryRepo = mApplication.entryRepo(viewMode);
    ROPregnancyRepo pregnancyRepo = mApplication.pregnancyRepo(viewMode);

    Flowable<List<CycleRenderer.RenderableCycle>> renderableCycleStream = Flowable.merge(Flowable.combineLatest(
            instructionsRepo.getAll()
                .distinctUntilChanged(),
            cycleRepo.getStream()
                .distinctUntilChanged(),
            (instructions, cycles) -> Flowable.merge(Flowable
                .fromIterable(cycles)
                .observeOn(Schedulers.computation())
                .parallel()
                .map(cycle -> Flowable.combineLatest(
                    entryRepo.getStreamForCycle(Flowable.just(cycle)),
                    cycleRepo.getPreviousCycle(cycle)
                        .map(Optional::of).defaultIfEmpty(Optional.empty())
                        .toFlowable(),
                    (entries, previousCycle) -> new CycleRenderer(cycle, previousCycle, entries, instructions).render())
                )
                .sequential()
                .toList()
                .toFlowable()
                .map(RxUtil::combineLatest))))
        .distinctUntilChanged()
        .cache();

    Flowable<List<CycleRenderer.CycleStats>> statsStream = renderableCycleStream
        .map(renderableCycles -> renderableCycles
            .stream()
            .map(CycleRenderer.RenderableCycle::stats)
            .collect(Collectors.toList()));

    Flowable<String> subtitleStream = Flowable.combineLatest(
        currentPageUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        statsStream,
        currentPageUpdates.flatMap(index -> cycleRepo.getStream()
            .firstOrError()
            .map(cycles -> cycles.get(index))
            .map(cycle -> Optional.ofNullable(cycle.pregnancyId))
            .flatMap(id -> !id.isPresent()
                ? Single.just(Optional.<Pregnancy>empty())
                : pregnancyRepo.get(id.get()).map(Optional::of).toSingle(Optional.empty()))
            .toObservable())
            .toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        (currentPage, stats, pregnancy) -> subtitle(stats, currentPage, LocalDate::now, pregnancy));

    return Flowable.combineLatest(
        currentPageUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        subtitleStream.distinctUntilChanged(),
        renderableCycleStream,
        mTargetLayoutMode.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        (currentPage, subtitle, renderableCycles, targetLayoutMode) -> new ViewState(currentPage, subtitle, renderableCycles, viewMode, targetLayoutMode));
  }

  Completable toggleLayoutMode() {
    return mCurrentLayoutMode.firstOrError().flatMapCompletable(currentViewMode -> {
      mTargetLayoutMode.onNext(Optional.of(currentViewMode == LayoutMode.GRID ? LayoutMode.LIST : LayoutMode.GRID));
      return Completable.complete();
    });
  }

  void completeLayoutTransition(LayoutMode layoutMode) {
    Optional<LayoutMode> currentTransition = mTargetLayoutMode.blockingFirst();
    if (!currentTransition.isPresent() || currentTransition.get() == layoutMode) {
      Timber.w("Corrupt layout transition! Expected %s, got %s", layoutMode.name(), currentTransition);
    }
    mCurrentLayoutMode.onNext(layoutMode);
    mTargetLayoutMode.onNext(Optional.empty());
  }

  void setViewMode(ViewMode viewMode) {
    Timber.d("Toggling view mode = %s", viewMode.name());
    mViewMode.onNext(viewMode);
  }

  public ViewMode currentViewMode() {
    return mViewMode.blockingFirst();
  }

  public LiveData<ViewState> viewStates() {
    return LiveDataReactiveStreams.fromPublisher(mViewStates
        .toFlowable(BackpressureStrategy.DROP)
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(viewState -> Timber.d("Publishing new ViewState")));
  }

  @VisibleForTesting
  static String subtitle(List<CycleRenderer.CycleStats> statsList, int index, Supplier<LocalDate> todaySupplier, Optional<Pregnancy> pregnancy) {
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
    if (!currentStats.daysPrePeak().isPresent()) {
      return "In prepeak phase";
    }
    if (currentStats.daysPostPeak().isPresent()) {
      return String.format(Locale.getDefault(), "Pre: %d Post: %d", currentStats.daysPrePeak().orElse(-1), currentStats.daysPostPeak().orElse(-1));
    }
    SummaryStatistics summaryStats = new SummaryStatistics();
    for (int i=index+1; i<statsList.size() && i <= 3; i++) {
      CycleRenderer.CycleStats s = statsList.get(i);
      summaryStats.addValue(statsList.get(i).daysPostPeak().orElse(0));
    }
    LocalDate peakDay = currentStats.cycleStartDate().plusDays(currentStats.daysPrePeak().orElse(0));
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
    final Optional<LayoutMode> targetLayoutMode;

    public final int currentCycleIndex;
    public final ViewMode viewMode;
    public final ImmutableList<CycleRenderer.RenderableCycle> renderableCycles;

    ViewState(int currentPage, String subtitle, List<CycleRenderer.RenderableCycle> renderableCycles, ViewMode viewMode, Optional<LayoutMode> targetLayoutMode) {
      this.title = currentPage == 0 ? "Current Cycle" : String.format("%d Cycles Ago", currentPage);
      this.subtitle = subtitle;
      this.showFab = currentPage == renderableCycles.size() - 1;
      this.viewMode = viewMode;
      this.targetLayoutMode = targetLayoutMode;

      this.currentCycleIndex = currentPage;
      this.renderableCycles = ImmutableList.copyOf(renderableCycles);
    }
  }

  public enum LayoutMode {
    GRID, LIST
  }

  public static class Factory implements ViewModelProvider.Factory {
    private final Application mApplication;
    private final ViewMode mViewMode;

    public Factory(Application application, ViewMode viewMode) {
      mApplication = application;
      mViewMode = viewMode;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new EntryListViewModel(mApplication, mViewMode);
    }
  }
}

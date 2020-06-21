package com.bloomcyclecare.cmcc.ui.cycle.vertical;

import android.app.Activity;
import android.app.Application;

import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.data.models.pregnancy.Pregnancy;
import com.bloomcyclecare.cmcc.data.repos.cycle.ROCycleRepo;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelection;
import com.bloomcyclecare.cmcc.ui.cycle.CycleListViewModel;
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
import androidx.lifecycle.ViewModelStoreOwner;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class CyclePageViewModel extends AndroidViewModel {

  public Subject<Integer> currentPageUpdates = BehaviorSubject.createDefault(0);

  private final ChartingApp mApplication;

  private final Subject<ViewState> mViewStates = BehaviorSubject.create();

  public static CyclePageViewModel create(ViewModelStoreOwner owner, Activity activity, CycleListViewModel cycleListViewModel) {
    return new ViewModelProvider(owner, new CyclePageViewModel.Factory(activity.getApplication(), cycleListViewModel))
        .get(CyclePageViewModel.class);
  }

  private CyclePageViewModel(@NonNull Application application, CycleListViewModel cycleListViewModel) {
    super(application);
    mApplication = ChartingApp.cast(application);

    cycleListViewModel.viewStateStream()
        .flatMap(viewState ->  Flowable.combineLatest(
            currentPageUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
            subtitleStream(viewState.viewMode(), Flowable.just(viewState.renderableCycles())).distinctUntilChanged().onErrorReturnItem("ERROR!"),
            Flowable.just(viewState.renderableCycles()),
            (currentPage, subtitle, renderableCycles) -> new ViewState(currentPage, subtitle, renderableCycles, viewState.viewMode())))
        .toObservable().subscribe(mViewStates);
  }

  public void focusDate(LocalDate dateToFocus) {
    ROCycleRepo cycleRepo = mApplication.cycleRepo(ViewMode.CHARTING);
    Single.zip(
        cycleRepo.getCycleForDate(dateToFocus).toSingle(),
        cycleRepo.getStream().firstOrError(),
        (cycleToFocus, cycles) -> cycles.indexOf(cycleToFocus))
        .subscribeOn(Schedulers.computation())
        .subscribe(index -> currentPageUpdates.onNext(index));
  }

  private Flowable<String> subtitleStream(
      ViewMode viewMode, Flowable<List<CycleRenderer.RenderableCycle>> renderableCycleStream) {
    switch (viewMode) {
      case TRAINING:
        return currentPageUpdates
            .toFlowable(BackpressureStrategy.BUFFER)
            .flatMap(index -> renderableCycleStream
                .map(renderableCycles -> renderableCycles.get(index))
                .map(renderableCycle -> {
                  int numWithStickers = 0;
                  for (CycleRenderer.RenderableEntry re : renderableCycle.entries()) {
                    StickerSelection selection = re.manualStickerSelection().orElse(null);
                    if (selection != null && !selection.isEmpty()) {
                      numWithStickers++;
                    }
                  }
                  int percentComplete = 100 * numWithStickers / renderableCycle.entries().size();
                  return String.format("%d%% complete", percentComplete);
                }));
      case CHARTING:
      case DEMO:
      default:
        Flowable<List<CycleRenderer.CycleStats>> statsStream = renderableCycleStream
            .map(renderableCycles -> renderableCycles
                .stream()
                .map(CycleRenderer.RenderableCycle::stats)
                .collect(Collectors.toList()));
        return Flowable.combineLatest(
            currentPageUpdates.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
            statsStream,
            currentPageUpdates.flatMap(index -> mApplication.cycleRepo(viewMode)
                .getStream()
                .firstOrError()
                .map(cycles -> cycles.get(index))
                .map(cycle -> Optional.ofNullable(cycle.pregnancyId))
                .flatMap(id -> !id.isPresent()
                    ? Single.just(Optional.<Pregnancy>empty())
                    : mApplication.pregnancyRepo(viewMode).get(id.get()).map(Optional::of).toSingle(Optional.empty()))
                .toObservable())
                .toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
            (currentPage, stats, pregnancy) -> subtitle(stats, currentPage, LocalDate::now, pregnancy));
    }
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

    public final int currentCycleIndex;
    public final ViewMode viewMode;
    @Deprecated
    public final ImmutableList<CycleRenderer.RenderableCycle> renderableCycles;

    ViewState(int currentPage, String subtitle, List<CycleRenderer.RenderableCycle> renderableCycles, ViewMode viewMode) {
      this.title = currentPage == 0 ? "Current Cycle" : String.format("%d Cycles Ago", currentPage);
      this.subtitle = subtitle;
      this.showFab = currentPage == renderableCycles.size() - 1;
      this.viewMode = viewMode;

      this.currentCycleIndex = currentPage;
      this.renderableCycles = ImmutableList.copyOf(renderableCycles);
    }
  }

  public enum LayoutMode {
    GRID, LIST
  }

  public static class Factory implements ViewModelProvider.Factory {
    private final Application mApplication;
    private final CycleListViewModel mCycleListViewModel;

    public Factory(Application application, CycleListViewModel cycleListViewModel) {
      mApplication = application;
      mCycleListViewModel = cycleListViewModel;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new CyclePageViewModel(mApplication, mCycleListViewModel);
    }
  }
}

package com.bloomcyclecare.cmcc.ui.cycle.grid;

import android.app.Application;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelection;
import com.bloomcyclecare.cmcc.data.models.training.Exercise;
import com.bloomcyclecare.cmcc.data.repos.entry.RWChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.sticker.RWStickerSelectionRepo;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.logic.chart.SelectionChecker;
import com.bloomcyclecare.cmcc.ui.cycle.CycleListViewModel;
import com.bloomcyclecare.cmcc.ui.cycle.RenderedEntry;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class EntryGridPageViewModel extends AndroidViewModel {

  private final CycleListViewModel mCycleListViewModel;
  private final Optional<Exercise> mExercise;
  private final Subject<ViewState> mViewStates = BehaviorSubject.create();
  private final Subject<RWChartEntryRepo> mEntryRepoSubject = BehaviorSubject.create();
  private final RWStickerSelectionRepo mStickerSelectionRepo;

  private EntryGridPageViewModel(@NonNull Application application, CycleListViewModel cycleListViewModel, Optional<Exercise.ID> exerciseID) {
    super(application);
    ChartingApp myApp = ChartingApp.cast(application);

    mCycleListViewModel = cycleListViewModel;
    mExercise = exerciseID.flatMap(Exercise::forID);
    if (mExercise.isPresent()) {
      mStickerSelectionRepo = myApp.stickerSelectionRepo(mExercise.get());
    } else {
      mStickerSelectionRepo = myApp.stickerSelectionRepo(ViewMode.CHARTING);
    }
    if (exerciseID.isPresent() && !mExercise.isPresent()) {
      Timber.w("Failed to find Exercise for ID: %s", exerciseID.get().name());
    }
    mCycleListViewModel.viewStateStream()
        .toObservable()
        .map(cycleListViewState -> {
          List<List<RenderedEntry>> lofl = new ArrayList<>(cycleListViewState.renderableCycles().size());
          List<CycleRenderer.RenderableCycle> renderableCycles = cycleListViewState.viewMode() == ViewMode.TRAINING
              ? cycleListViewState.renderableCycles()
              : Lists.reverse(cycleListViewState.renderableCycles());
          for (CycleRenderer.RenderableCycle rc : renderableCycles) {
            List<RenderedEntry> renderedEntries = new ArrayList<>(rc.entries().size());
            for (CycleRenderer.RenderableEntry re : rc.entries()) {
              renderedEntries.add(RenderedEntry.create(
                  re, cycleListViewState.autoStickeringEnabled(), cycleListViewState.viewMode(), false));
            }
            lofl.add(renderedEntries);
          }
          return ViewState.create(
              lofl,
              getSubtitle(cycleListViewState, getStatViews(renderableCycles)),
              cycleListViewState.viewMode());
        })
        .subscribe(mViewStates);
  }

  public enum Stat {
    FLOW,
    POC,
    PEAK,
    END
  }

  static class StatView {
    public final String summary;
    public final ImmutableSet<Integer> days;

    StatView(String summary, ImmutableSet<Integer> days) {
      this.summary = summary;
      this.days = days;
    }
  }

  private ImmutableMap<Stat, StatView> getStatViews(Collection<CycleRenderer.RenderableCycle> renderableCycles) {
    int maxCycleLength = 35;
    List<CycleRenderer.RenderableCycle> cycles = new ArrayList<>();
    Timber.v("Getting stats for %d cycles", renderableCycles.size());
    for (CycleRenderer.RenderableCycle cycle : renderableCycles.stream()
        .sorted((c1, c2) -> c2.cycle().compareTo(c1.cycle()))
        .collect(Collectors.toList())) {
      if (cycle.entries().size() <= maxCycleLength) {
        cycles.add(cycle);
      } else {
        Timber.v("Cycle starting %s had %d entries which exceeded the limit of %d", cycle.cycle().startDateStr, cycle.entries().size(), maxCycleLength);
        break;
      }
    }
    Timber.v("Only considering last %d cycles", cycles.size());
    ImmutableMap.Builder<Stat, StatView> builder = ImmutableMap.builder();

    builder.put(Stat.FLOW, getStatView(
        "Typical flow length: ", cycles,
        (c) -> c.stats().daysOfFlow() < c.entries().size(),  // Only look at cases where we are "past" the flow
        (c) -> c.stats().daysOfFlow()));
    //noinspection OptionalGetWithoutIsPresent
    builder.put(Stat.POC, getStatView(
        "Typical PoC: ", cycles,
        (c) -> c.stats().daysBeforePoC().isPresent(),  // Only look at cases where we are "past" the PoC
        (c) -> c.stats().daysBeforePoC().get() + 1));
    //noinspection OptionalGetWithoutIsPresent
    builder.put(Stat.PEAK, getStatView(
        "Typical peak day: ", cycles,
        (c) -> c.stats().daysPrePeak().isPresent(),  // Only consider cycles which have had a peak day
        (c) -> c.stats().daysPrePeak().get() + 1));
    builder.put(Stat.END, getStatView(
        "Typical cycle length: ", cycles,
        (c) -> c.cycle().endDate != null,  // Only consider cycles which have ended
        (c) -> c.entries().size()));

    return builder.build();
  }

  private String getStatSummary(Collection<Integer> days) {
    SummaryStatistics stats = new SummaryStatistics();
    days.forEach(stats::addValue);
    long mean = Math.round(stats.getMean());
    double ci95 = 1.960 * stats.getStandardDeviation() / Math.sqrt(stats.getN());
    return String.format(Locale.getDefault(), "%d±%.1f", mean, ci95);
  }

  private StatView getStatView(
      String summaryPrefix,
      Collection<CycleRenderer.RenderableCycle> cycles,
      Predicate<CycleRenderer.RenderableCycle> filterFn,
      Function<CycleRenderer.RenderableCycle, Integer> mapperFn) {
    ImmutableSet<Integer> days = ImmutableSet.copyOf(cycles.stream()
        .filter(filterFn)
        .map(mapperFn)
        .collect(Collectors.toSet()));
    return new StatView(summaryPrefix + getStatSummary(days), days);
  }

  private String getSubtitle(CycleListViewModel.ViewState viewState, ImmutableMap<Stat, StatView> statViews) {
    switch (viewState.viewMode()) {
      case TRAINING:
        int entriesWithCorrectAnswer = 0;
        int entriesWithMarker = 0;
        for (CycleRenderer.RenderableCycle rc : viewState.renderableCycles()) {
          for (CycleRenderer.RenderableEntry re : rc.entries()) {
            if (!Strings.isNullOrEmpty(re.trainingMarker())) {
              entriesWithMarker++;
            }
            Optional<StickerSelection> manualSelection = re.manualStickerSelection();
            if (manualSelection.isPresent()) {
              if (SelectionChecker.create(re.stickerSelectionContext()).check(manualSelection.get()).ok()) {
                entriesWithCorrectAnswer++;
              }
            }
          }
        }
        if (entriesWithMarker == 0) {
          return "No entries to check!";
        }
        long percentComplete = 100 * entriesWithCorrectAnswer / entriesWithMarker;
        return String.format("%d%% complete", percentComplete);
      default:
        List<String> statSummaries = statViews.values().stream()
            .map(sv -> sv.summary)
            .collect(Collectors.toList());
        return Joiner.on(", ").join(statSummaries);
    }
  }

  ViewMode currentViewMode() {
    return mCycleListViewModel.currentViewMode();
  }

  Completable updateSticker(LocalDate entryDate, StickerSelection selection) {
    return mStickerSelectionRepo.recordSelection(selection, entryDate);
  }

  LiveData<ViewState> viewStates() {
    return LiveDataReactiveStreams.fromPublisher(mViewStates.toFlowable(BackpressureStrategy.BUFFER));
  }

  @AutoValue
  public static abstract class ViewState {

    public abstract List<List<RenderedEntry>> renderedEntries();
    public abstract String subtitle();
    public abstract ViewMode viewMode();

    public static ViewState create(List<List<RenderedEntry>> renderedEntries, String subtitle, ViewMode viewMode) {
      return new AutoValue_EntryGridPageViewModel_ViewState(renderedEntries, subtitle, viewMode);
    }

  }

  public static class Factory implements ViewModelProvider.Factory {
    private final Application mApplication;
    private final CycleListViewModel mCycleListViewModel;
    private final Optional<Exercise.ID> mExerciseID;

    Factory(Application application, CycleListViewModel cycleListViewModel, EntryGridPageFragmentArgs args) {
      mApplication = application;
      mCycleListViewModel = cycleListViewModel;
      int exerciseIdOrdinal = args.getExerciseIdOrdinal();
      mExerciseID = Optional.ofNullable(exerciseIdOrdinal < 0 ? null : Exercise.ID.values()[exerciseIdOrdinal]);
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new EntryGridPageViewModel(mApplication, mCycleListViewModel, mExerciseID);
    }
  }
}

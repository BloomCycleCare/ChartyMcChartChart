package com.bloomcyclecare.cmcc.ui.cycle.grid;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

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
import com.bloomcyclecare.cmcc.ui.cycle.stickers.StickerSelectionViewModel;
import com.bloomcyclecare.cmcc.utils.HeatMap;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.ReplaySubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class EntryGridPageViewModel extends AndroidViewModel {

  private final CycleListViewModel mCycleListViewModel;
  private final Optional<Exercise> mExercise;
  private final Subject<ViewState> mViewStates = BehaviorSubject.create();
  private final Subject<RWChartEntryRepo> mEntryRepoSubject = BehaviorSubject.create();
  private final Subject<Boolean> mStatToggles = ReplaySubject.create(100);

  private final StickerSelectionViewModel mStickerSelectionViewModel;

  private static final String HEADER_COLOR_EMPTY = "#FFFFFF";
  private static final List<String> HEADER_COLORS = ImmutableList.of(
      "#CBDAF5", "#A9C2F0", "#759FE5", "#477AD1", "#1F59C4");

  private EntryGridPageViewModel(@NonNull Application application, CycleListViewModel cycleListViewModel, Optional<Exercise.ID> exerciseID) {
    super(application);
    ChartingApp myApp = ChartingApp.cast(application);

    mCycleListViewModel = cycleListViewModel;
    mExercise = exerciseID.flatMap(Exercise::forID);
    if (mExercise.isPresent()) {
      mStickerSelectionViewModel = StickerSelectionViewModel.forExercise(mExercise.get(), myApp);
    } else {
      mStickerSelectionViewModel = StickerSelectionViewModel.forViewMode(ViewMode.CHARTING, myApp);
    }
    if (exerciseID.isPresent() && !mExercise.isPresent()) {
      Timber.w("Failed to find Exercise for ID: %s", exerciseID.get().name());
    }
    mCycleListViewModel.viewStateStream()
        .toObservable()
        .distinctUntilChanged()
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

          HeatMap heatMap = cycleListViewState.statView()
              .map(sv -> new HeatMap(sv.dayCounts, HEADER_COLOR_EMPTY, HEADER_COLORS))
              .orElse(new HeatMap(ImmutableMap.of(), HEADER_COLOR_EMPTY, HEADER_COLORS));
          return ViewState.create(
              lofl,
              getSubtitle(cycleListViewState, cycleListViewState.subtitle()),
              cycleListViewState.viewMode(),
              heatMap);
        })
        .subscribe(mViewStates);
  }

  void toggleStats() {
    mStatToggles.onNext(true);
  }

  private String getSubtitle(CycleListViewModel.ViewState viewState, String subtitle) {
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
        return subtitle;
    }
  }

  ViewMode currentViewMode() {
    return mCycleListViewModel.currentViewMode();
  }

  StickerSelectionViewModel stickerSelectionViewModel() {
    return mStickerSelectionViewModel;
  }

  LiveData<ViewState> viewStates() {
    return LiveDataReactiveStreams.fromPublisher(mViewStates.toFlowable(BackpressureStrategy.BUFFER));
  }

  @AutoValue
  public static abstract class ViewState {

    public abstract List<List<RenderedEntry>> renderedEntries();
    public abstract String subtitle();
    public abstract ViewMode viewMode();
    public abstract HeatMap headerColors();

    public static ViewState create(List<List<RenderedEntry>> renderedEntries, String subtitle, ViewMode viewMode, HeatMap headerColors) {
      return new AutoValue_EntryGridPageViewModel_ViewState(renderedEntries, subtitle, viewMode, headerColors);
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

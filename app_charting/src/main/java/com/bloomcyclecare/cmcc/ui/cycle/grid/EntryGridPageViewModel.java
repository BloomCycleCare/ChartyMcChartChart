package com.bloomcyclecare.cmcc.ui.cycle.grid;

import android.app.Application;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.data.models.stickering.Sticker;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelection;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerText;
import com.bloomcyclecare.cmcc.data.models.training.Exercise;
import com.bloomcyclecare.cmcc.data.repos.entry.RWChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.sticker.RWStickerSelectionRepo;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.logic.chart.SelectionChecker;
import com.bloomcyclecare.cmcc.ui.cycle.CycleListViewModel;
import com.bloomcyclecare.cmcc.ui.cycle.RenderedEntry;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
          for (CycleRenderer.RenderableCycle rc : cycleListViewState.renderableCycles()) {
            List<RenderedEntry> renderedEntries = new ArrayList<>(rc.entries().size());
            for (CycleRenderer.RenderableEntry re : rc.entries()) {
              renderedEntries.add(RenderedEntry.create(
                  re, cycleListViewState.autoStickeringEnabled(), cycleListViewState.viewMode()));
            }
            lofl.add(renderedEntries);
          }
          return ViewState.create(
              lofl,
              getSubtitle(cycleListViewState),
              cycleListViewState.viewMode());
        })
        .subscribe(mViewStates);
  }

  private String getSubtitle(CycleListViewModel.ViewState viewState) {
    switch (viewState.viewMode()) {
      case TRAINING:
        int entriesWithCorrectAnswer = 0;
        int entriesWithMarker = 0;
        for (CycleRenderer.RenderableCycle rc : viewState.renderableCycles()) {
          for (CycleRenderer.RenderableEntry re : rc.entries()) {
            if (!Strings.isNullOrEmpty(re.trainingMarker())) {
              entriesWithMarker++;
            }
            if (re.manualStickerSelection().isPresent()) {
              StickerSelection expected = StickerSelection.create(
                  Sticker.fromStickerColor(re.backgroundColor(), re.showBaby()),
                  viewState.viewMode() == ViewMode.TRAINING ? null : StickerText.fromString(re.peakDayText()));
              if (SelectionChecker.create(expected).check(re.manualStickerSelection().get()).ok()) {
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
        return "";
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

package com.bloomcyclecare.cmcc.ui.cycle.grid;

import android.app.Application;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.models.Exercise;
import com.bloomcyclecare.cmcc.data.models.StickerSelection;
import com.bloomcyclecare.cmcc.data.repos.entry.RWChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.sticker.RWStickerSelectionRepo;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.ui.cycle.CycleListViewModel;
import com.bloomcyclecare.cmcc.ui.cycle.RenderedEntry;
import com.google.auto.value.AutoValue;

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
    mCycleListViewModel = cycleListViewModel;
    mStickerSelectionRepo = MyApplication.cast(application).stickerSelectionRepo(ViewMode.CHARTING);
    mExercise = exerciseID.flatMap(Exercise::forID);
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
        long cyclesWithSticker = viewState.renderableCycles().stream()
            .map(renderableCycle -> {
              for (CycleRenderer.RenderableEntry re : renderableCycle.entries()) {
                if (re.manualStickerSelection().isPresent() && !re.manualStickerSelection().get().isEmpty()) {
                  return true;
                }
              }
              return false;
            })
            .filter(v -> v)
            .count();
        long percentComplete = 100 * cyclesWithSticker / viewState.renderableCycles().size();
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

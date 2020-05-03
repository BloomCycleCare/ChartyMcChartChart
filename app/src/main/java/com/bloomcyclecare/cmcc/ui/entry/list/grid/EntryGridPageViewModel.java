package com.bloomcyclecare.cmcc.ui.entry.list.grid;

import android.app.Application;
import android.os.Bundle;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.models.Exercise;
import com.bloomcyclecare.cmcc.data.models.StickerSelection;
import com.bloomcyclecare.cmcc.data.repos.cycle.ROCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.entry.RWChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.instructions.ROInstructionsRepo;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.utils.RxUtil;
import com.google.auto.value.AutoValue;

import org.joda.time.LocalDate;

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
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class EntryGridPageViewModel extends AndroidViewModel {

  private final MyApplication mApp;
  private final Optional<Exercise> mExercise;
  private final Subject<ViewMode> mViewMode = BehaviorSubject.create();
  private final Subject<ViewState> mViewStates = BehaviorSubject.create();
  private final Subject<RWChartEntryRepo> mEntryRepoSubject = BehaviorSubject.create();

  private EntryGridPageViewModel(@NonNull Application application, ViewMode viewMode, Optional<Exercise.ID> exerciseID) {
    super(application);
    mApp = MyApplication.cast(application);
    mExercise = exerciseID.flatMap(Exercise::forID);
    if (exerciseID.isPresent() && !mExercise.isPresent()) {
      Timber.w("Failed to find Exercise for ID: %s", exerciseID.get().name());
    }
    mViewMode.onNext(viewMode);
    mViewMode.map(this::viewStateStream).switchMap(Flowable::toObservable).subscribe(mViewStates);
  }

  private Flowable<ViewState> viewStateStream(ViewMode viewMode) {
    ROInstructionsRepo instructionsRepo = mApp.instructionsRepo(viewMode);
    ROCycleRepo cycleRepo = mExercise.isPresent()
        ? mApp.cycleRepo(mExercise.get())
        : mApp.cycleRepo(viewMode);
    RWChartEntryRepo entryRepo = mApp.entryRepo(viewMode);
    mEntryRepoSubject.onNext(entryRepo);

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
                entryRepo.getStreamForCycle(Flowable.just(cycle)).doOnNext(l -> Timber.i("NEXT!")).doOnComplete(() -> Timber.i("COMPLETE!")),
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

    Flowable<String> subtitleStream = renderableCycleStream
        .flatMap(renderableCycles -> Flowable.fromIterable(renderableCycles)
            .map(renderableCycle -> {
              for (CycleRenderer.RenderableEntry re : renderableCycle.entries()) {
                StickerSelection selection = re.entry().stickerSelection;
                if (selection != null && !selection.isEmpty()) {
                  return true;
                }
              }
              return false;
            })
            .toList()
            .map(bitField -> {
              long daysWithStickers = bitField.stream().filter(v -> v).count();
              long percentComplete = 100 * daysWithStickers / bitField.size();
              return String.format("%d%% complete", percentComplete);
            })
            .toFlowable());

    return Flowable.combineLatest(
        renderableCycleStream.distinctUntilChanged(),
        subtitleStream.distinctUntilChanged(),
        (renderableCycles, subtitle) -> ViewState.create(renderableCycles, subtitle, viewMode));
  }

  ViewMode currentViewMode() {
    return mViewMode.blockingFirst();
  }

  Completable updateSticker(LocalDate entryDate, StickerSelection selection) {
    return mEntryRepoSubject.firstOrError()
        .flatMapCompletable(entryRepo -> entryRepo.updateStickerSelection(entryDate, selection));
  }

  LiveData<ViewState> viewStates() {
    return LiveDataReactiveStreams.fromPublisher(mViewStates.toFlowable(BackpressureStrategy.BUFFER));
  }

  @AutoValue
  public static abstract class ViewState {

    public abstract List<CycleRenderer.RenderableCycle> renderableCycles();
    public abstract String subtitle();
    public abstract ViewMode viewMode();

    public static ViewState create(List<CycleRenderer.RenderableCycle> renderableCycles, String subtitle, ViewMode viewMode) {
      return new AutoValue_EntryGridPageViewModel_ViewState(renderableCycles, subtitle, viewMode);
    }

  }

  public static class Factory implements ViewModelProvider.Factory {
    private final Application mApplication;
    private final ViewMode mViewMode;
    private final Optional<Exercise.ID> mExerciseID;

    Factory(Application application, Bundle arguments) {
      mApplication = application;
      int viewModeOrdinal = arguments.getInt(ViewMode.class.getCanonicalName(), -1);
      mViewMode = viewModeOrdinal < 0 ? ViewMode.TRAINING : ViewMode.values()[viewModeOrdinal];

      int exerciseIdOrdinal = arguments.getInt(Exercise.ID.class.getCanonicalName(), -1);
      mExerciseID = Optional.ofNullable(exerciseIdOrdinal < 0 ? null : Exercise.ID.values()[exerciseIdOrdinal]);
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new EntryGridPageViewModel(mApplication, mViewMode, mExerciseID);
    }
  }
}

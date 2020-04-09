package com.bloomcyclecare.cmcc.ui.entry.list;

import android.app.Application;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.repos.cycle.CycleRepos;
import com.bloomcyclecare.cmcc.data.repos.cycle.ROCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.entry.ChartEntryRepos;
import com.bloomcyclecare.cmcc.data.repos.entry.ROChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.instructions.InstructionsRepos;
import com.bloomcyclecare.cmcc.data.repos.instructions.ROInstructionsRepo;
import com.bloomcyclecare.cmcc.logic.chart.CycleRenderer;
import com.bloomcyclecare.cmcc.logic.chart.ObservationParser;
import com.google.auto.value.AutoValue;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

class FragmentViewModel extends AndroidViewModel {

  private final Subject<ViewState> mViewState = BehaviorSubject.create();
  private final Subject<ScrollState> mScrollEventsFromUI = BehaviorSubject.create();

  private FragmentViewModel(@NonNull Application application, boolean trainingMode, Cycle cycle) {
    super(application);
    try {
      MyApplication myApp = MyApplication.cast(application);
      ROCycleRepo cycleRepo = trainingMode ? CycleRepos.forTraining() : myApp.cycleRepo();
      ROInstructionsRepo instructionsRepo = trainingMode ? InstructionsRepos.forTraining() : myApp.instructionsRepo();
      ROChartEntryRepo entryRepo = trainingMode ? ChartEntryRepos.forTraining() : myApp.entryRepo();

      Flowable<CycleRenderer.RenderableCycle> cycleStream = Flowable.combineLatest(
          cycleRepo.getPreviousCycle(cycle).map(Optional::of).defaultIfEmpty(Optional.empty()).toFlowable(),
          instructionsRepo.getAll(),
          entryRepo.getStreamForCycle(Flowable.just(cycle)),
          (previousCycle, instructions, entries) -> new CycleRenderer(cycle, previousCycle, entries, instructions))
          .map(CycleRenderer::render);

      Flowable<ScrollState> scrollStateFlowable = mScrollEventsFromUI
          .sample(200, TimeUnit.MILLISECONDS)
          .toFlowable(BackpressureStrategy.BUFFER);

      Flowable.combineLatest(
          cycleStream.distinctUntilChanged().doOnNext(rc -> Timber.v("Got new RenderableCycle")),
          scrollStateFlowable.distinctUntilChanged().doOnNext(ss -> Timber.v("Got new ScrollState")),
          (renderableCycle, scrollState) -> new ViewState(cycle, renderableCycle, scrollState, trainingMode))
          .toObservable()
          .subscribeOn(Schedulers.computation())
          .subscribe(mViewState);
    } catch (ObservationParser.InvalidObservationException ioe) {
      Timber.wtf(ioe);
    }
  }

  void updateScrollState(ScrollState scrollState) {
    mScrollEventsFromUI.onNext(scrollState);
  }

  public LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(mViewState
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
        .toFlowable(BackpressureStrategy.BUFFER));
  }

  public static class ViewState {
    final Cycle cycle;
    final CycleRenderer.RenderableCycle renderableCycle;
    final ScrollState scrollState;
    final boolean trainingMode;

    private ViewState(Cycle cycle, CycleRenderer.RenderableCycle renderableCycle, ScrollState scrollState, boolean trainingMode) {
      this.cycle = cycle;
      this.renderableCycle = renderableCycle;
      this.scrollState = scrollState;
      this.trainingMode = trainingMode;
    }
  }

  @AutoValue
  public static abstract class ScrollState {
    abstract int firstVisibleDay();
    abstract int offsetPixels();

    @Override
    public String toString() {
      return ("First visible: " + firstVisibleDay() + " Offset: " + offsetPixels());
    }

    public static ScrollState create(int firstVisibleDay, int offsetPixels) {
      return new AutoValue_FragmentViewModel_ScrollState(firstVisibleDay, offsetPixels);
    }
  }

  public static class Factory implements ViewModelProvider.Factory {

    private final Application mApplication;
    private final boolean isTrainingMode;
    private final Cycle currentCycle;

    public Factory(Application application, boolean isTrainingMode, Cycle currentCycle) {
      mApplication = application;
      this.isTrainingMode = isTrainingMode;
      this.currentCycle = currentCycle;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return (T) new FragmentViewModel(mApplication, isTrainingMode, currentCycle);
    }
  }
}

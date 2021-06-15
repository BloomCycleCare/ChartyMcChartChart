package com.bloomcyclecare.cmcc.ui.main;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;

import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.backup.AppStateExporter;
import com.bloomcyclecare.cmcc.data.repos.cycle.ROCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.instructions.ROInstructionsRepo;
import com.bloomcyclecare.cmcc.logic.PreferenceRepo;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class MainViewModel extends AndroidViewModel {

  private final Subject<String> mTitleSubject = BehaviorSubject.createDefault("Some title");
  private final Subject<String> mSubtitleSubject = BehaviorSubject.createDefault("");
  private final Subject<ViewState> mViewStateSubject = BehaviorSubject.create();

  private final ROInstructionsRepo mInstructionsRepo;
  private final ROCycleRepo mCycleRepo;

  public MainViewModel(@NonNull Application application) {
    super(application);

    ChartingApp myApp = ChartingApp.cast(application);
    PreferenceRepo preferenceRepo = myApp.preferenceRepo();
    mInstructionsRepo = myApp.instructionsRepo(ViewMode.CHARTING);
    mCycleRepo = myApp.cycleRepo(ViewMode.CHARTING);

    Flowable.combineLatest(
        mTitleSubject.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        mSubtitleSubject.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        preferenceRepo.summaries()
            .map(summary -> summary.defaultToDemoMode() ? ViewMode.DEMO : ViewMode.CHARTING)
            .distinctUntilChanged(),
        myApp.pregnancyRepo(ViewMode.CHARTING).getAll()
            .map(l -> !l.isEmpty()),
        ViewState::create)
        .toObservable()
        .subscribe(mViewStateSubject);
  }

  private Single<Boolean> appInitialized() {
    return Single.zip(
        mCycleRepo.getCurrentCycle().map(c -> true).switchIfEmpty(Single.just(false)),
        mInstructionsRepo.getAll().firstOrError().map(instructions -> !instructions.isEmpty()),
        (hasCycle, hasInstructions) -> {
          boolean appInitialized = hasCycle && hasInstructions;
          if (appInitialized) {
            return true;
          }
          Timber.i("App not initialized: hasCycle %b, hasInstructions %b", hasCycle, hasInstructions);
          return false;
        })
        .subscribeOn(Schedulers.computation());
  }

  private Single<Boolean> hasExistingData() {
    return Single.zip(
        mCycleRepo.getStream().first(ImmutableList.of()),
        mInstructionsRepo.getAll().first(ImmutableList.of()),
        (cycles, instructionSets) -> {
          if (!cycles.isEmpty()) {
            Timber.w("Found %d cycles", cycles.size());
          }
          if (!instructionSets.isEmpty()) {
            Timber.w("Found %d instruction sets", instructionSets.size());
          }
          return !cycles.isEmpty() || !instructionSets.isEmpty();
        });
  }

  private Completable maybeExportData(Single<Boolean> exportExistingData, Activity activity) {
    return hasExistingData()
        .flatMap(hasExistingData -> !hasExistingData ? Single.just(false) : exportExistingData)
        .flatMapCompletable(export -> {
          if (!export) {
            return Completable.complete();
          }
          AppStateExporter exporter = new AppStateExporter(ChartingApp.cast(activity.getApplication()));
          return exporter.getShareIntent(activity)
              .flatMapCompletable(intent -> Completable.fromRunnable(() -> activity.startActivity(intent)));
        });
  }

  Single<Boolean> shouldShowCyclePage(Single<Boolean> exportExistingData, Activity activity) {
    return appInitialized().flatMap(
        initialized -> initialized
            ? Single.just(true) : maybeExportData(exportExistingData, activity).andThen(Single.just(false)));
  }

  Single<ViewState> initialState() {
    return mViewStateSubject.firstOrError();
  }

  public LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(
        mViewStateSubject.toFlowable(BackpressureStrategy.BUFFER));
  }

  public void updateTitle(String value) {
    mTitleSubject.onNext(value);
  }

  public void updateSubtitle(String value) {
    mSubtitleSubject.onNext(value);
  }

  @AutoValue
  public static abstract class ViewState {
    public abstract String title();
    public abstract String subtitle();
    public abstract ViewMode defaultViewMode();
    public abstract boolean showPregnancyMenuItem();

    public static ViewState create(String title, String subtitle, ViewMode defaultViewMode, boolean showPregnancyMenuItem) {
      return new AutoValue_MainViewModel_ViewState(title, subtitle, defaultViewMode, showPregnancyMenuItem);
    }

  }
}

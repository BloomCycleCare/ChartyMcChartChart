package com.bloomcyclecare.cmcc.ui.main;

import android.app.Application;

import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.apps.charting.ViewMode;
import com.bloomcyclecare.cmcc.data.repos.cycle.ROCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.instructions.ROInstructionsRepo;
import com.bloomcyclecare.cmcc.logic.PreferenceRepo;
import com.google.auto.value.AutoValue;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

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

  Single<Boolean> appInitialized() {
    return Single.zip(
        mCycleRepo.getCurrentCycle().map(c -> true).switchIfEmpty(Single.just(false)),
        mInstructionsRepo.getAll().firstOrError().map(instructions -> !instructions.isEmpty()),
        (hasCycle, hasInstructions) -> hasCycle && hasInstructions)
        .subscribeOn(Schedulers.computation());
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

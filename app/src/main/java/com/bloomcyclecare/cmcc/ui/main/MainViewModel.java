package com.bloomcyclecare.cmcc.ui.main;

import android.app.Application;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.application.ViewMode;
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
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class MainViewModel extends AndroidViewModel {

  private final Subject<String> mTitleSubject = BehaviorSubject.createDefault("Some title");
  private final Subject<String> mSubtitleSubject = BehaviorSubject.createDefault("Some subtitle");
  private final Subject<ViewState> mViewStateSubject = BehaviorSubject.create();

  private final ROInstructionsRepo mInstructionsRepo;

  public MainViewModel(@NonNull Application application) {
    super(application);

    MyApplication myApp = MyApplication.cast(application);
    PreferenceRepo preferenceRepo = myApp.preferenceRepo();
    mInstructionsRepo = myApp.instructionsRepo(ViewMode.CHARTING);

    Flowable.combineLatest(
        mTitleSubject.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        mSubtitleSubject.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        preferenceRepo.summaries()
            .map(summary -> summary.defaultToDemoMode() ? ViewMode.DEMO : ViewMode.CHARTING)
            .distinctUntilChanged(),
        ViewState::create)
        .toObservable()
        .subscribe(mViewStateSubject);
  }

  Single<Boolean> instructionsInitialized() {
    return mInstructionsRepo.getAll()
        .firstOrError()
        .map(instructions -> !instructions.isEmpty());
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

    public static ViewState create(String title, String subtitle, ViewMode defaultViewMode) {
      return new AutoValue_MainViewModel_ViewState(title, subtitle, defaultViewMode);
    }

  }
}

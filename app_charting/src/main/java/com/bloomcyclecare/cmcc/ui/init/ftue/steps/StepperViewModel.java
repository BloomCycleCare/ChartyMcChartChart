package com.bloomcyclecare.cmcc.ui.init.ftue.steps;

import android.app.Application;

import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.apps.charting.ViewMode;
import com.bloomcyclecare.cmcc.data.repos.cycle.ROCycleRepo;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class StepperViewModel extends AndroidViewModel {

  private final ROCycleRepo mCycleRepo;
  private final Map<TosItem, Boolean> mTosAgreements = new ConcurrentHashMap<>();

  private final Subject<ViewState> mViewStateSubject = BehaviorSubject.create();
  private final Subject<ImmutableMap<TosItem, Boolean>> mTosAgreementSubject = BehaviorSubject.createDefault(ImmutableMap.copyOf(mTosAgreements));

  public StepperViewModel(@NonNull Application application) {
    super(application);

    mCycleRepo = ChartingApp.cast(application).cycleRepo(ViewMode.CHARTING);

    Flowable.combineLatest(
        mTosAgreementSubject.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged(),
        mCycleRepo.getStream().map(cycles -> !cycles.isEmpty()),
        ViewState::create)
        .toObservable()
        .subscribe(mViewStateSubject);
  }

  public LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(
        mViewStateSubject.toFlowable(BackpressureStrategy.BUFFER));
  }

  public Observable<ViewState> viewStateStream() {
    return mViewStateSubject;
  }

  public Single<ViewState> currentViewState() {
    return mViewStateSubject.firstOrError();
  }

  void recordTosAgreement(TosItem tosItem, boolean agreed) {
    mTosAgreements.put(tosItem, agreed);
    mTosAgreementSubject.onNext(ImmutableMap.copyOf(mTosAgreements));
  }

  @AutoValue
  public static abstract class ViewState {
    abstract Map<TosItem, Boolean> tosItems();
    abstract boolean hasCycle();

    public static ViewState create(Map<TosItem, Boolean> tosItems, boolean hasCycle) {
      return new AutoValue_StepperViewModel_ViewState(tosItems, hasCycle);
    }

  }
}

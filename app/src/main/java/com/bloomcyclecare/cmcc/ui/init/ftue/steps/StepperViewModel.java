package com.bloomcyclecare.cmcc.ui.init.ftue.steps;

import android.app.Application;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.repos.cycle.ROCycleRepo;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class StepperViewModel extends AndroidViewModel {

  private final Subject<ViewState> mViewStateSubject = BehaviorSubject.create();
  private final Subject<Boolean> mTosCompleteSubject = BehaviorSubject.createDefault(false);

  private final ROCycleRepo mCycleRepo;

  public StepperViewModel(@NonNull Application application) {
    super(application);

    mCycleRepo = MyApplication.cast(application).cycleRepo(ViewMode.CHARTING);

    Flowable.combineLatest(
        mTosCompleteSubject.toFlowable(BackpressureStrategy.BUFFER).distinctUntilChanged()
        .map(complete -> {
          if (!complete) {
            return ImmutableMap.of();
          }
          ImmutableMap.Builder<TosItem, Boolean> builder = ImmutableMap.builder();
          for (TosItem tosItem : TosItem.values()) {
            builder.put(tosItem, true);
          }
          return builder.build();
        }),
        mCycleRepo.getStream().map(cycles -> !cycles.isEmpty()),
        ViewState::create)
        .toObservable()
        .subscribe(mViewStateSubject);
  }

  public LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(
        mViewStateSubject.toFlowable(BackpressureStrategy.BUFFER));
  }

  public Single<ViewState> currentViewState() {
    return mViewStateSubject.firstOrError();
  }

  public void recordTosComplete() {
    mTosCompleteSubject.onNext(true);
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

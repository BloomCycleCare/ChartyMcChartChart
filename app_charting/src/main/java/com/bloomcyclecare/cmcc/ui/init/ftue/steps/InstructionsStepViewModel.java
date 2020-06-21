package com.bloomcyclecare.cmcc.ui.init.ftue.steps;

import android.app.Application;

import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.data.models.instructions.Instructions;
import com.bloomcyclecare.cmcc.data.repos.cycle.ROCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.instructions.RWInstructionsRepo;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.SingleSubject;
import io.reactivex.subjects.Subject;

public class InstructionsStepViewModel extends AndroidViewModel {

  private final RWInstructionsRepo mInstructionsRepo;
  private final ROCycleRepo mCycleRepo;

  private final SingleSubject<Instructions> mInitialInstructions = SingleSubject.create();
  private final Subject<ViewState> mViewStateSubject = BehaviorSubject.create();

  public InstructionsStepViewModel(@NonNull Application application) {
    super(application);
    mInstructionsRepo = ChartingApp.cast(application).instructionsRepo(ViewMode.CHARTING);
    mCycleRepo = ChartingApp.cast(application).cycleRepo(ViewMode.CHARTING);

    mInstructionsRepo
        .getAll()
        .firstOrError()
        .flatMapMaybe(list -> {
          if (list.isEmpty()) {
            return Maybe.empty();
          }
          return Maybe.just(list.get(0));
        })
        .switchIfEmpty(mCycleRepo
            .getStream()
            .filter(list -> !list.isEmpty())
            .map(list -> list.get(0))
            .take(1)
            .singleOrError()
            .flatMap(currentCycle -> {
              Instructions instructions = new Instructions(currentCycle.startDate, ImmutableList.of(), ImmutableList.of(), ImmutableList.of());
              return mInstructionsRepo
                  .insertOrUpdate(instructions)
                  .andThen(Single.just(instructions));
            }))
        .subscribe(mInitialInstructions);

    mInitialInstructions.flatMapObservable(instructions -> mInstructionsRepo
        .get(instructions.startDate)
        .toObservable()
        .map(ViewState::create))
        .subscribe(mViewStateSubject);
  }

  LiveData<ViewState> viewState() {
    return LiveDataReactiveStreams.fromPublisher(
        mViewStateSubject.toFlowable(BackpressureStrategy.BUFFER));
  }

  Single<Instructions> initialInstructions() {
    return mInitialInstructions;
  }

  Completable commit() {
    return mInstructionsRepo.commit();
  }

  @AutoValue
  public static abstract class ViewState {

    public abstract Instructions instructions();

    public static ViewState create(Instructions instructions) {
      return new AutoValue_InstructionsStepViewModel_ViewState(instructions);
    }
  }
}

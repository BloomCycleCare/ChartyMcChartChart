package com.roamingroths.cmcc.ui.instructions;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.entities.Instructions;
import com.roamingroths.cmcc.data.repos.CycleRepo;
import com.roamingroths.cmcc.data.repos.InstructionsRepo;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

public class InstructionsListViewModel extends AndroidViewModel {

  private final BehaviorSubject<Boolean> mHasStartedNewInsructions = BehaviorSubject.createDefault(false);
  private final BehaviorSubject<Optional<Instructions>> mInstructionsForFocus = BehaviorSubject.createDefault(Optional.absent());

  private final InstructionsRepo mInstructionsRepo;
  private final CycleRepo mCycleRepo;

  public InstructionsListViewModel(@NonNull Application application) {
    super(application);

    MyApplication myApp = MyApplication.cast(application);
    mInstructionsRepo = myApp.instructionsRepo();
    mCycleRepo = new CycleRepo(myApp.db());
  }

  Single<Boolean> isDirty() {
    return mInstructionsRepo.isDirty();
  }

  Completable save() {
    return mInstructionsRepo.commit();
  }

  Completable clearPending() {
    return mInstructionsRepo.clearPending();
  }

  Completable createNewInstructions(LocalDate newInstructionsStartDate) {
    return Completable.defer(() -> {
      mHasStartedNewInsructions.onNext(true);
      Instructions newInstructions = new Instructions(newInstructionsStartDate, ImmutableList.of(), ImmutableList.of());
      return mInstructionsRepo.insertOrUpdate(newInstructions);
    });
  }

  Completable addPreviousInstructions(LocalDate newInstructionsStartDate) {
    return Completable.defer(() -> {
      Instructions newInstructions = new Instructions(newInstructionsStartDate, ImmutableList.of(), ImmutableList.of());
        mInstructionsForFocus.onNext(Optional.of(newInstructions));
        return mInstructionsRepo.insertOrUpdate(newInstructions);
      });
  }

  Completable delete(Instructions instructions) {
    return mInstructionsRepo.delete(instructions).flatMapCompletable(instructionsOut -> {
      if (!instructionsOut.equals(instructions)) {
        mInstructionsForFocus.onNext(Optional.of(instructionsOut));
        if (instructions.startDate.equals(LocalDate.now())) {
          mHasStartedNewInsructions.onNext(false);
        }
      }
      return Completable.complete();
    });
  }

  LiveData<ViewState> viewState() {
    Flowable<List<Instructions>> instructionsStream = mInstructionsRepo
        .getAll()
        .distinctUntilChanged()
        .flatMap(instructions -> {
          if (instructions.isEmpty()) {
            Timber.i("No existing entries, adding one for current cycle.");
            return mCycleRepo
                .getCurrentCycle().toSingle()
                .flatMapPublisher(currentCycle -> {
                  List<Instructions> updatedInstructions = new ArrayList<>(instructions);
                  Instructions newInstructions = new Instructions(currentCycle.startDate, ImmutableList.of(), ImmutableList.of());
                  updatedInstructions.add(newInstructions);
                  Timber.i("Inserting instructions for cycle beginning %s", currentCycle.startDate);
                  return mInstructionsRepo
                      .insertOrUpdate(newInstructions)
                      .andThen(Flowable.just(updatedInstructions));
                });
          }
          return Flowable.just(instructions);
        })
        .flatMap(instructions -> Flowable
            .fromIterable(instructions)
            .sorted((a, b) -> b.startDate.compareTo(a.startDate))
            .toList()
            .toFlowable())
        .subscribeOn(Schedulers.computation());

    return LiveDataReactiveStreams.fromPublisher(Flowable.combineLatest(
        instructionsStream.doOnNext(i -> Timber.i("New set of instructions")),
        mHasStartedNewInsructions.toFlowable(BackpressureStrategy.BUFFER),
        mInstructionsForFocus.toFlowable(BackpressureStrategy.BUFFER),
        ViewState::new));
  }

  public void clearFocus() {
    mInstructionsForFocus.onNext(Optional.absent());
  }

  static class ViewState {
    public final List<Instructions> instructions;
    public final boolean hasStartedNewInstructions;
    @Nullable public final Instructions instructionsForFocus;

    ViewState(List<Instructions> instructions, boolean hasStartedNewInstructions, Optional<Instructions> instructionsForFocus) {
      this.instructions = instructions;
      this.hasStartedNewInstructions = hasStartedNewInstructions;
      this.instructionsForFocus = instructionsForFocus.orNull();
    }
  }
}

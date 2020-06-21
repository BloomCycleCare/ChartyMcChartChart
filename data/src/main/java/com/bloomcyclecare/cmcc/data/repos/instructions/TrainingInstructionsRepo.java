package com.bloomcyclecare.cmcc.data.repos.instructions;

import com.bloomcyclecare.cmcc.data.models.instructions.Instructions;
import com.bloomcyclecare.cmcc.data.models.training.TrainingCycle;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import timber.log.Timber;

public class TrainingInstructionsRepo implements RWInstructionsRepo {

  private final List<Instructions> mInstrtionsList;

  TrainingInstructionsRepo(List<TrainingCycle> trainingCycles, Supplier<LocalDate> today) {
    Deque<TrainingCycle> trainingStack = new ArrayDeque<>();
    for (TrainingCycle trainingCycle : trainingCycles) {
      trainingStack.push(trainingCycle);
    }

    Deque<Instructions> instructions = new ArrayDeque<>();
    Iterator<TrainingCycle> reversedTrainingCycles = trainingStack.iterator();
    while (reversedTrainingCycles.hasNext()) {
      TrainingCycle trainingCycle = reversedTrainingCycles.next();
      if (trainingCycle.entries().isEmpty()) {
        Timber.w("Skipping empty training cycle");
        continue;
      }
      LocalDate endDate = instructions.size() == 0 ? today.get() : instructions.peek().startDate.minusDays(1);
      LocalDate startDate = endDate.minusDays(trainingCycle.entries().size() - 1);
      Instructions i = new Instructions(trainingCycle.instructions);
      i.startDate = startDate;
      instructions.push(i);
    }
    mInstrtionsList = ImmutableList.copyOf(instructions);
  }

  @Override
  public Flowable<Instructions> get(LocalDate date) {
    Instructions out = null;
    for (Instructions instructions : mInstrtionsList) {
      if (date.isBefore(instructions.startDate)) {
        continue;
      }
      if (out == null || instructions.startDate.isAfter(out.startDate)) {
        out = instructions;
      }
    }
    return out == null ? Flowable.empty() : Flowable.just(out);
  }

  @Override
  public Flowable<List<Instructions>> getAll() {
    return Flowable.just(mInstrtionsList);
  }

  @Override
  public Single<Boolean> hasAnyAfter(LocalDate date) {
    return get(date).map(instructions -> {
      int index = mInstrtionsList.indexOf(instructions);
      if (index < 0) {
        throw new IllegalStateException();
      }
      return index < mInstrtionsList.size() - 1;
    }).first(false);
  }

  @Override
  public Flowable<UpdateEvent> updateEvents() {
    return Flowable.<UpdateEvent>empty()
        .doOnSubscribe(s -> Timber.w("No updates will be provided from training repo"));
  }

  @Override
  public Single<Instructions> delete(Instructions instructions) {
    return Single.error(
        new UnsupportedOperationException("Updates not supported on training repo"));
  }

  @Override
  public Completable deleteAll() {
    return Completable.error(
        new UnsupportedOperationException("Updates not supported on training repo"));
  }

  @Override
  public Completable insertOrUpdate(Instructions instructions) {
    return Completable.error(
        new UnsupportedOperationException("Updates not supported on training repo"));
  }

  @Override
  public Single<Boolean> isDirty() {
    return Single.just(false)
        .doOnSubscribe(s -> Timber.w("Training repo will never be dirty"));
  }

  @Override
  public Completable commit() {
    return Completable.complete()
        .doOnSubscribe(s -> Timber.w("Training repo will never have something to commit"));
  }

  @Override
  public Completable clearPending() {
    return Completable.complete()
        .doOnSubscribe(s -> Timber.w("Training repo will never have pending operations"));
  }
}

package com.bloomcyclecare.cmcc.data.repos.instructions;

import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.bloomcyclecare.cmcc.data.models.TrainingCycle;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import timber.log.Timber;

public class TrainingInstructionsRepo implements RWInstructionsRepo {

  private final List<Instructions> mInstrtionsList;

  TrainingInstructionsRepo(List<TrainingCycle> trainingCycles) {
    Set<Instructions> instructions = new LinkedHashSet<>();
    for (TrainingCycle trainingCycle : trainingCycles) {
      instructions.add(trainingCycle.instructions);
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

package com.bloomcyclecare.cmcc.data.repos.instructions;

import com.bloomcyclecare.cmcc.data.entities.Instructions;

import org.joda.time.LocalDate;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import timber.log.Timber;

public class TrainingInstructionsRepo implements RWInstructionsRepo {
  @Override
  public Flowable<Instructions> get(LocalDate startDate) {
    return null;
  }

  @Override
  public Flowable<List<Instructions>> getAll() {
    return null;
  }

  @Override
  public Single<Boolean> hasAnyAfter(LocalDate date) {
    return null;
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

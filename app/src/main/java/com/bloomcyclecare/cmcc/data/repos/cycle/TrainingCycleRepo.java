package com.bloomcyclecare.cmcc.data.repos.cycle;

import com.bloomcyclecare.cmcc.data.entities.Cycle;

import org.joda.time.LocalDate;

import java.util.List;

import androidx.core.util.Consumer;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import timber.log.Timber;

public class TrainingCycleRepo implements RWCycleRepo {

  @Override
  public Flowable<List<Cycle>> getStream() {
    return null;
  }

  @Override
  public Maybe<Cycle> getPreviousCycle(Cycle cycle) {
    return null;
  }

  @Override
  public Maybe<Cycle> getNextCycle(Cycle cycle) {
    return null;
  }

  @Override
  public Maybe<Cycle> getCurrentCycle() {
    return null;
  }

  @Override
  public Maybe<Cycle> getLatestCycle() {
    return null;
  }

  @Override
  public Maybe<Cycle> getCycleForDate(LocalDate date) {
    return null;
  }

  @Override
  public Single<SplitResult> splitCycle(Cycle cycleToSplit, LocalDate firstDayOfNewCycle) {
    return Single.error(
        new UnsupportedOperationException("Updates not supported on training repo"));
  }

  @Override
  public Single<SplitResult> splitCycle(Cycle cycleToSplit, LocalDate firstDayOfNewCycle, Consumer<Cycle> fieldUpdater) {
    return Single.error(
        new UnsupportedOperationException("Updates not supported on training repo"));
  }

  @Override
  public Single<Cycle> joinCycle(Cycle cycleToJoin, JoinType joinType) {
    return Single.error(
        new UnsupportedOperationException("Updates not supported on training repo"));
  }

  @Override
  public Flowable<UpdateEvent> updateEvents() {
    return Flowable.<UpdateEvent>empty()
        .doOnSubscribe(s -> Timber.w("No updates will be provided from training repo"));
  }

  @Override
  public Completable deleteAll() {
    return Completable.error(
        new UnsupportedOperationException("Updates not supported on training repo"));
  }

  @Override
  public Completable delete(Cycle cycle) {
    return Completable.error(
        new UnsupportedOperationException("Updates not supported on training repo"));
  }

  @Override
  public Completable insertOrUpdate(Cycle cycle) {
    return Completable.error(
        new UnsupportedOperationException("Updates not supported on training repo"));
  }
}

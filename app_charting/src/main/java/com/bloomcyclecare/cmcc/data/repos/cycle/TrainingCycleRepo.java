package com.bloomcyclecare.cmcc.data.repos.cycle;

import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.models.training.TrainingCycle;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import androidx.core.util.Consumer;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import timber.log.Timber;

public class TrainingCycleRepo implements RWCycleRepo {

  private final ImmutableList<Cycle> mCycles;

  TrainingCycleRepo(List<TrainingCycle> trainingCycles, Supplier<LocalDate> today) {
    Deque<TrainingCycle> trainingStack = new ArrayDeque<>();
    for (TrainingCycle trainingCycle : trainingCycles) {
      trainingStack.push(trainingCycle);
    }

    Deque<Cycle> cycles = new ArrayDeque<>();
    Iterator<TrainingCycle> reversedTrainingCycles = trainingStack.iterator();
    while (reversedTrainingCycles.hasNext()) {
      TrainingCycle trainingCycle = reversedTrainingCycles.next();
      if (trainingCycle.entries().isEmpty()) {
        Timber.w("Skipping empty training cycle");
        continue;
      }
      LocalDate endDate = cycles.size() == 0 ? today.get() : cycles.peek().startDate.minusDays(1);
      LocalDate startDate = endDate.minusDays(trainingCycle.entries().size() - 1);
      cycles.push(new Cycle("training", startDate, endDate.equals(today.get()) ? null : endDate, null));
    }
    mCycles = ImmutableList.copyOf(cycles.iterator());
  }

  @Override
  public Flowable<List<Cycle>> getStream() {
    return Flowable.just(mCycles);
  }

  @Override
  public Maybe<Cycle> getPreviousCycle(Cycle cycle) {
    int index = mCycles.indexOf(cycle);
    if (index <= 0) {
      return Maybe.empty();
    }
    return Maybe.just(mCycles.get(index - 1));
  }

  @Override
  public Maybe<Cycle> getNextCycle(Cycle cycle) {
    int index = mCycles.indexOf(cycle);
    if (index < 0 || index == mCycles.size() - 1) {
      return Maybe.empty();
    }
    return Maybe.just(mCycles.get(index + 1));
  }

  @Override
  public Maybe<Cycle> getCurrentCycle() {
    if (mCycles.isEmpty()) {
      return Maybe.empty();
    }
    return Maybe.just(mCycles.get(mCycles.size() - 1));
  }

  @Override
  public Maybe<Cycle> getLatestCycle() {
    return getCurrentCycle();
  }

  @Override
  public Maybe<Cycle> getCycleForDate(LocalDate date) {
    for (Cycle cycle : mCycles) {
      if (cycle.startDate.isAfter(date)) {
        break;
      }
      if (cycle.endDate != null && cycle.endDate.isBefore(date)) {
        continue;
      }
      if (!cycle.startDate.isAfter(date)) {
        return Maybe.just(cycle);
      }
    }
    return Maybe.empty();
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

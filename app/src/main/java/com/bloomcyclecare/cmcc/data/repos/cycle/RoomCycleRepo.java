package com.bloomcyclecare.cmcc.data.repos.cycle;

import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.db.CycleDao;
import com.bloomcyclecare.cmcc.data.entities.Cycle;

import org.joda.time.LocalDate;

import java.util.List;

import androidx.core.util.Consumer;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;

class RoomCycleRepo implements RWCycleRepo {

  private final CycleDao cycleDao;
  private final PublishSubject<RWCycleRepo.UpdateEvent> updates = PublishSubject.create();

  RoomCycleRepo(AppDatabase db) {
    cycleDao = db.cycleDao();
  }

  @Override
  public Flowable<RWCycleRepo.UpdateEvent> updateEvents() {
    return updates.toFlowable(BackpressureStrategy.BUFFER);
  }

  @Override
  public Flowable<List<Cycle>> getStream() {
    return cycleDao
        .getStream()
        .distinctUntilChanged();
  }

  @Override
  public Maybe<Cycle> getPreviousCycle(Cycle cycle) {
    return cycleDao.getCycleWithEndDate(cycle.startDate.minusDays(1));
  }

  @Override
  public Maybe<Cycle> getNextCycle(Cycle cycle) {
    if (cycle.endDate == null) {
      return Maybe.error(new IllegalArgumentException("Cycle has no endDate"));
    }
    return cycleDao.getCycleForDate(cycle.endDate.plusDays(1));
  }

  @Override
  public Maybe<Cycle> getCurrentCycle() {
    return cycleDao.getCurrentCycle();
  }

  @Override
  public Maybe<Cycle> getLatestCycle() {
    return getStream().firstOrError().flatMapMaybe(cycles -> {
      if (cycles.isEmpty()) {
        return Maybe.empty();
      }
      Cycle latestCycle = null;
      for (Cycle c : cycles) {
        if (latestCycle == null || c.startDate.isAfter(latestCycle.startDate)) {
          latestCycle = c;
        }
      }
      return Maybe.just(latestCycle);
    });
  }

  @Override
  public Maybe<Cycle> getCycleForDate(LocalDate date) {
    return cycleDao.getCycleForDate(date);
  }

  @Override
  public Single<RWCycleRepo.SplitResult> splitCycle(Cycle cycleToSplit, LocalDate firstDayOfNewCycle) {
    return splitCycle(cycleToSplit, firstDayOfNewCycle, newCycle -> {});
  }

  @Override
  public Single<RWCycleRepo.SplitResult> splitCycle(Cycle cycleToSplit, LocalDate firstDayOfNewCycle, Consumer<Cycle> fieldUpdater) {
    Cycle newCycle = new Cycle("asdf", firstDayOfNewCycle, cycleToSplit.endDate, null);
    fieldUpdater.accept(newCycle);
    Cycle copyOfCycleToSplit = new Cycle(cycleToSplit);
    copyOfCycleToSplit.endDate = firstDayOfNewCycle.minusDays(1);
    return insertOrUpdate(newCycle)
        .andThen(insertOrUpdate(copyOfCycleToSplit))
        .andThen(Single.just(new RWCycleRepo.SplitResult(newCycle, copyOfCycleToSplit)));
  }

  @Override
  public Single<Cycle> joinCycle(Cycle cycleToJoin, RWCycleRepo.JoinType joinType) {
    switch (joinType) {
      case WITH_NEXT:
        return getNextCycle(cycleToJoin)
            .toSingle()
            .flatMap(nextCycle -> {
              Cycle copyOfCycleToJoin = new Cycle(cycleToJoin);
              copyOfCycleToJoin.endDate = nextCycle.endDate;
              copyOfCycleToJoin.pregnancyId = nextCycle.pregnancyId;
              return insertOrUpdate(copyOfCycleToJoin)
                  .andThen(delete(nextCycle))
                  .andThen(Single.just(copyOfCycleToJoin));
            });
      case WITH_PREVIOUS:
        return getPreviousCycle(cycleToJoin)
            .toSingle()
            .flatMap(previousCycle -> {
              Cycle copyOfPreviousCycle = new Cycle(previousCycle);
              copyOfPreviousCycle.endDate = cycleToJoin.endDate;
              copyOfPreviousCycle.pregnancyId = cycleToJoin.pregnancyId;
              return insertOrUpdate(copyOfPreviousCycle)
                  .andThen(delete(cycleToJoin))
                  .andThen(Single.just(copyOfPreviousCycle));
            });
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  public Completable deleteAll() {
    return getStream()
        .firstOrError()
        .flatMapObservable(Observable::fromIterable)
        .flatMapCompletable(this::delete)
        .doOnSubscribe(s -> Timber.d("Deleting all cycles"))
        .doOnComplete(() -> Timber.d("Done deleting all cycles"))
        ;
  }

  @Override
  public Completable delete(Cycle cycle) {
    return cycleDao.delete(cycle)
        .doOnComplete(() -> updates.onNext(RWCycleRepo.UpdateEvent.forCycle(cycle)));
  }

  @Override
  public Completable insertOrUpdate(Cycle cycle) {
    return cycleDao.insert(cycle)
        .doOnComplete(() -> updates.onNext(RWCycleRepo.UpdateEvent.forCycle(cycle)));
  }
}

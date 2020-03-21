package com.bloomcyclecare.cmcc.data.repos;

import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.db.CycleDao;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.google.common.base.Optional;
import com.google.common.collect.Range;

import org.joda.time.DateTime;
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

public class CycleRepo {

  private final CycleDao cycleDao;
  private final PublishSubject<UpdateEvent> updates = PublishSubject.create();

  public CycleRepo(AppDatabase db) {
    cycleDao = db.cycleDao();
  }

  public Flowable<UpdateEvent> updateEvents() {
    return updates.toFlowable(BackpressureStrategy.BUFFER);
  }

  public Flowable<List<Cycle>> getStream() {
    return cycleDao
        .getStream()
        .distinctUntilChanged();
  }

  public Maybe<Cycle> getPreviousCycle(Cycle cycle) {
    return cycleDao.getCycleWithEndDate(cycle.startDate.minusDays(1));
  }

  public Maybe<Cycle> getNextCycle(Cycle cycle) {
    if (cycle.endDate == null) {
      return Maybe.error(new IllegalArgumentException("Cycle has no endDate"));
    }
    return cycleDao.getCycleForDate(cycle.endDate.plusDays(1));
  }

  public Maybe<Cycle> getCurrentCycle() {
    return cycleDao.getCurrentCycle();
  }

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

  public static class SplitResult {
    public final Cycle newCycle;
    public final Cycle previousCycle;

    SplitResult(Cycle newCycle, Cycle previousCycle) {
      this.newCycle = newCycle;
      this.previousCycle = previousCycle;
    }
  }

  public Single<SplitResult> splitCycle(Cycle cycleToSplit, LocalDate firstDayOfNewCycle) {
    return splitCycle(cycleToSplit, firstDayOfNewCycle, newCycle -> {});
  }

  public Single<SplitResult> splitCycle(Cycle cycleToSplit, LocalDate firstDayOfNewCycle, Consumer<Cycle> fieldUpdater) {
    Cycle newCycle = new Cycle("asdf", firstDayOfNewCycle, cycleToSplit.endDate, null);
    fieldUpdater.accept(newCycle);
    Cycle copyOfCycleToSplit = new Cycle(cycleToSplit);
    copyOfCycleToSplit.endDate = firstDayOfNewCycle.minusDays(1);
    return insertOrUpdate(newCycle)
        .andThen(insertOrUpdate(copyOfCycleToSplit))
        .andThen(Single.just(new SplitResult(newCycle, copyOfCycleToSplit)));
  }

  public enum JoinType {
    WITH_PREVIOUS, WITH_NEXT;
  }

  public Single<Cycle> joinCycle(Cycle cycleToJoin, JoinType joinType) {
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


  public Completable deleteAll() {
    return getStream()
        .firstOrError()
        .flatMapObservable(Observable::fromIterable)
        .flatMapCompletable(this::delete)
        .doOnSubscribe(s -> Timber.d("Deleting all cycles"))
        .doOnComplete(() -> Timber.d("Done deleting all cycles"))
        ;
  }

  public Completable delete(Cycle cycle) {
    return cycleDao.delete(cycle)
        .doOnComplete(() -> updates.onNext(UpdateEvent.forCycle(cycle)));
  }

  public Completable insertOrUpdate(Cycle cycle) {
    return cycleDao.insert(cycle)
        .doOnComplete(() -> updates.onNext(UpdateEvent.forCycle(cycle)));
  }

  public Maybe<Cycle> getCycleForDate(LocalDate date) {
    return cycleDao.getCycleForDate(date);
  }

  public static class UpdateEvent {
    public final DateTime updateTime;
    public final Range<LocalDate> dateRange;

    private UpdateEvent(DateTime updateTime, Range<LocalDate> dateRange) {
      this.updateTime = updateTime;
      this.dateRange = dateRange;
    }

    static UpdateEvent forCycle(Cycle cycle) {
      return new UpdateEvent(DateTime.now(), Range.closed(
          cycle.startDate, Optional.fromNullable(cycle.endDate).or(LocalDate.now())));
    }
  }
}

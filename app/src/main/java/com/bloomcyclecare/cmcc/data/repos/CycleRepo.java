package com.bloomcyclecare.cmcc.data.repos;

import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.db.CycleDao;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.google.common.base.Optional;
import com.google.common.collect.Range;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
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

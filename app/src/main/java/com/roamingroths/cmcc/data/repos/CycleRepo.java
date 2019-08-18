package com.roamingroths.cmcc.data.repos;

import com.roamingroths.cmcc.data.db.AppDatabase;
import com.roamingroths.cmcc.data.db.CycleDao;
import com.roamingroths.cmcc.data.entities.Cycle;

import org.joda.time.LocalDate;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class CycleRepo {

  private final CycleDao cycleDao;

  public CycleRepo(AppDatabase db) {
    cycleDao = db.cycleDao();
  }

  public Flowable<List<Cycle>> getStream() {
    return cycleDao
        .getStream()
        .distinctUntilChanged();
  }

  public Maybe<Cycle> getPreviousCycle(Cycle cycle) {
    return cycleDao.getCycleWithEndDate(cycle.startDate.minusDays(1));
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
    return cycleDao.delete(cycle);
  }

  public Completable insertOrUpdate(Cycle cycle) {
    return cycleDao.insert(cycle);
  }

  public Single<Cycle> startNewCycle(Cycle currentCycle, LocalDate startDate) {
    return Single
        .fromCallable(() -> cycleDao.startNewCycle(currentCycle, startDate))
        .subscribeOn(Schedulers.computation());
  }
}

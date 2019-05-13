package com.roamingroths.cmcc.data.repos;

import com.roamingroths.cmcc.data.db.AppDatabase;
import com.roamingroths.cmcc.data.db.CycleDao;
import com.roamingroths.cmcc.data.entities.Cycle;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
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

  public Maybe<Cycle> getCurrentCycle() {
    return cycleDao.getCurrentCycle();
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
}

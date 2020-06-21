package com.bloomcyclecare.cmcc.data.repos.pregnancy;

import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.db.PregnancyDao;
import com.bloomcyclecare.cmcc.data.models.charting.Cycle;
import com.bloomcyclecare.cmcc.data.models.pregnancy.Pregnancy;
import com.bloomcyclecare.cmcc.data.repos.cycle.RWCycleRepo;
import com.google.common.base.Preconditions;

import org.joda.time.LocalDate;

import java.util.List;
import java.util.Optional;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

class RoomPregnancyRepo implements RWPregnancyRepo {

  private final RWCycleRepo mCycleRepo;
  private final PregnancyDao mPregnancyDao;

  RoomPregnancyRepo(AppDatabase database, RWCycleRepo mCycleRepo) {
    this.mCycleRepo = mCycleRepo;
    this.mPregnancyDao = database.pregnancyDao();
  }

  @Override
  public Flowable<List<Pregnancy>> getAll() {
    return mPregnancyDao.getAll()
        .distinctUntilChanged()
        .subscribeOn(Schedulers.computation());
  }

  @Override
  public Maybe<Pregnancy> get(Long id) {
    return mPregnancyDao.getById(id);
  }

  public Completable insertAll(List<Pregnancy> pregnancies) {
    return mPregnancyDao.insert(pregnancies)
        .subscribeOn(Schedulers.computation());
  }

  @Override
  public Completable update(Pregnancy pregnancy) {
    return mPregnancyDao.update(pregnancy)
        .subscribeOn(Schedulers.computation());
  }

  @Override
  public Completable delete(Pregnancy pregnancy) {
    return mPregnancyDao.delete(pregnancy)
        .subscribeOn(Schedulers.computation());
  }

  @Override
  public Completable startPregnancy(LocalDate dateOfTest) {
    return Single.merge(Single.zip(
        createPregnancy(dateOfTest),
        mCycleRepo.getCurrentCycle().map(Optional::of).switchIfEmpty(Single.just(Optional.empty())),
        (pregnancy, currentCycle) ->  {
          LocalDate cycleStartDate = dateOfTest.plusDays(1);
          if (currentCycle.isPresent()) {
            return mCycleRepo.splitCycle(
                currentCycle.get(), cycleStartDate, newCycle -> newCycle.pregnancyId = pregnancy.id);
          }
          Cycle newCycle = new Cycle("new", cycleStartDate, null, pregnancy.id);
          return mCycleRepo.insertOrUpdate(newCycle).andThen(Single.just(newCycle));
        }))
        .ignoreElement()
        .subscribeOn(Schedulers.computation());
  }

  @Override
  public Completable revertPregnancy(LocalDate dateOfTest) {
    return mCycleRepo.getCycleForDate(dateOfTest)
        .toSingle()
        .flatMapCompletable(currentCycle -> mCycleRepo
            .joinCycle(currentCycle, RWCycleRepo.JoinType.WITH_NEXT)
            .ignoreElement()
            .andThen(Completable.defer(() -> dropPregnancy(currentCycle.pregnancyId))
                .doOnError(t -> Timber.e("Could not delete pregnancy"))
                .onErrorComplete()))
        .subscribeOn(Schedulers.computation());
  }

  private Completable dropPregnancy(Long id) {
    Preconditions.checkNotNull(id);
    return mPregnancyDao.getById(id).toSingle().flatMapCompletable(this::delete);
  }

  private Single<Pregnancy> createPregnancy(LocalDate dateOfTest) {
    Pregnancy pregnancy = new Pregnancy();
    pregnancy.positiveTestDate = dateOfTest;
    return mPregnancyDao.insert(pregnancy).map(id -> {
      pregnancy.id = id;
      return pregnancy;
    });
  }
}

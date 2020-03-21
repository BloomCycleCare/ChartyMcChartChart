package com.bloomcyclecare.cmcc.data.repos;

import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.db.PregnancyDao;
import com.bloomcyclecare.cmcc.data.entities.Pregnancy;
import com.google.common.base.Preconditions;

import org.joda.time.LocalDate;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import timber.log.Timber;

public class PregnancyRepo {

  private final CycleRepo mCycleRepo;
  private final PregnancyDao mPregnancyDao;

  public PregnancyRepo(AppDatabase database, CycleRepo mCycleRepo) {
    this.mCycleRepo = mCycleRepo;
    this.mPregnancyDao = database.pregnancyDao();
  }

  public Single<List<Pregnancy>> getAll() {
    return mPregnancyDao.getAll();
  }

  public Completable insertAll(List<Pregnancy> pregnancies) {
    return mPregnancyDao.insert(pregnancies);
  }

  public Completable startPregnancy(LocalDate dateOfTest) {
    return Single.merge(Single.zip(
        createPregnancy(dateOfTest),
        mCycleRepo.getCycleForDate(dateOfTest).toSingle(),
        ((pregnancy, currentCycle) -> mCycleRepo.splitCycle(currentCycle, dateOfTest.plusDays(1), newCycle -> newCycle.pregnancyId = pregnancy.id))))
        .ignoreElement();
  }

  public Completable revertPregnancy(LocalDate dateOfTest) {
    return mCycleRepo.getCycleForDate(dateOfTest)
        .toSingle()
        .flatMapCompletable(currentCycle -> mCycleRepo
            .joinCycle(currentCycle, CycleRepo.JoinType.WITH_NEXT)
            .ignoreElement()
            .andThen(Completable.defer(() -> dropPregnancy(currentCycle.pregnancyId))
                .doOnError(t -> Timber.e("Could not delete pregnancy"))
                .onErrorComplete()));
  }

  private Completable dropPregnancy(Long id) {
    Preconditions.checkNotNull(id);
    return mPregnancyDao.getById(id).toSingle().flatMapCompletable(mPregnancyDao::delete);
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

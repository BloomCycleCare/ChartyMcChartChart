package com.bloomcyclecare.cmcc.data.repos.medication;

import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.db.MedicationDao;
import com.bloomcyclecare.cmcc.data.models.medication.Medication;

import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class RoomMedicationRepo implements RWMedicationRepo {

  private final MedicationDao medicationDao;

  RoomMedicationRepo(AppDatabase database) {
    medicationDao = database.medicationDao();
  }

  @Override
  public Completable importAll(List<Medication> medications) {
    return Completable.merge(medications.stream()
        .map(medication -> medicationDao.insert(medication).ignoreElement())
        .collect(Collectors.toSet()));
  }

  @Override
  public Single<Medication> save(Medication medication) {
    if (medication.id <= 0) {
      Timber.v("Mediation has no ID, inserting");
      return medicationDao.insert(medication)
          .map(id -> {
            Medication m = new Medication(medication);
            m.id = id;
            return m;
          })
          .doOnSuccess(m -> Timber.v("New ID %d", m.id))
          .subscribeOn(Schedulers.computation());
    }
    Timber.v("Mediation has ID, updating");
    return medicationDao.update(medication)
        .andThen(Single.just(medication))
        .subscribeOn(Schedulers.computation());
  }

  @Override
  public Completable delete(Medication medication) {
    Timber.v("Deleting Medication with ID: %d, name: %s", medication.id, medication.name);
    return medicationDao.delete(medication)
        .subscribeOn(Schedulers.computation());
  }

  @Override
  public Flowable<List<Medication>> getAll(boolean includeInactive) {
    return medicationDao.get().map(medications -> medications
        .stream()
        .filter(m -> m.active || includeInactive)
        .collect(Collectors.toList()))
        .subscribeOn(Schedulers.computation());
  }
}

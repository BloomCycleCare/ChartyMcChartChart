package com.bloomcyclecare.cmcc.data.repos.medication;

import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.db.MedicationDao;
import com.bloomcyclecare.cmcc.data.models.medication.Medication;
import com.bloomcyclecare.cmcc.data.models.medication.MedicationWithRelations;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

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
  public Completable importAll(@Nullable List<Medication> medications) {
    if (medications == null) {
      return Completable.complete();
    }
    return Completable.merge(medications.stream()
        .map(medication -> medicationDao.insert(medication).ignoreElement())
        .collect(Collectors.toSet()));
  }

  @Override
  public Single<Medication> save(Medication medication) {
    if (medication.id() <= 0) {
      Timber.v("Mediation has no ID, inserting");
      return medicationDao.insert(medication)
          .map(id -> {
            Medication m = Medication.create(id, medication.name(), medication.description());
            return m;
          })
          .doOnSuccess(m -> Timber.v("New ID %d", m.id()))
          .subscribeOn(Schedulers.computation());
    }
    Timber.v("Mediation has ID, updating");
    return medicationDao.update(medication)
        .andThen(Single.just(medication))
        .subscribeOn(Schedulers.computation());
  }

  @Override
  public Completable delete(Medication medication) {
    Timber.v("Deleting Medication with ID: %d, name: %s", medication.id(), medication.name());
    return medicationDao.delete(medication)
        .subscribeOn(Schedulers.computation());
  }

  @Override
  public Flowable<List<Medication>> getAll(boolean includeInactive) {
    return getAllWithRelations(includeInactive)
        .map(medications -> medications.stream()
            .map(MedicationWithRelations::medication)
            .collect(Collectors.toList()))
        .subscribeOn(Schedulers.computation());
  }

  @Override
  public Flowable<List<MedicationWithRelations>> getAllWithRelations(boolean includeInactive) {
    return medicationDao.getDecorated().map(medications -> {
      if (includeInactive) {
        return medications;
      }
      return medications.stream()
          .filter(MedicationWithRelations::hasActivePrescription)
          .collect(Collectors.toList());
    }).subscribeOn(Schedulers.computation());
  }
}

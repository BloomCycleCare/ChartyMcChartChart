package com.bloomcyclecare.cmcc.data.repos.medication;

import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.db.MedicationDao;
import com.bloomcyclecare.cmcc.data.models.medication.Medication;

import io.reactivex.Single;
import timber.log.Timber;

public class RoomMedicationRepo implements RWMedicationRepo {

  private final MedicationDao medicationDao;

  RoomMedicationRepo(AppDatabase database) {
    medicationDao = database.medicationDao();
  }

  @Override
  public Single<Medication> save(Medication medication) {
    if (medication.id <= 0) {
      Timber.v("Mediation has no ID, inserting");
      return medicationDao.insert(medication).doOnSuccess(m -> Timber.v("New ID %d", m.id));
    }
    Timber.v("Mediation has ID, updating");
    return medicationDao.update(medication).andThen(Single.just(medication));
  }
}

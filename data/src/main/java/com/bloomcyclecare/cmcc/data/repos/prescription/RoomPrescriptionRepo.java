package com.bloomcyclecare.cmcc.data.repos.prescription;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bloomcyclecare.cmcc.data.db.PrescriptionDao;
import com.bloomcyclecare.cmcc.data.models.medication.Medication;
import com.bloomcyclecare.cmcc.data.models.medication.Prescription;
import com.google.common.collect.ImmutableList;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

public class RoomPrescriptionRepo implements RWPrescriptionRepo {

  private final PrescriptionDao mPrescriptionDao;

  public RoomPrescriptionRepo(PrescriptionDao prescriptionDao) {
    mPrescriptionDao = prescriptionDao;
  }

  @Override
  public Observable<ImmutableList<Prescription>> getPrescriptions(@Nullable Medication medication) {
    if (medication == null) {
      return Observable.just(ImmutableList.of());
    }
    return mPrescriptionDao.getAll((int) medication.id()).map(ImmutableList::copyOf).toObservable();
  }

  @Override
  public Single<Prescription> save(@NonNull Prescription prescription) {
    return mPrescriptionDao.save(prescription).map(id -> Prescription.create(
        id.intValue(), prescription.startDate(), prescription.endDate(), prescription.dosage(),
        prescription.takeInMorning(), prescription.takeAtNoon(), prescription.takeInEvening(),
        prescription.takeAtNight(), prescription.takeAsNeeded()));
  }

  @Override
  public Completable delete(@NonNull Prescription prescription) {
    return mPrescriptionDao.delete(prescription);
  }
}

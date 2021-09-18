package com.bloomcyclecare.cmcc.data.repos.medication;

import com.bloomcyclecare.cmcc.data.models.medication.Medication;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface RWMedicationRepo extends ROMedicationRepo {

  Single<Medication> save(Medication medication);

  Completable delete(Medication medication);
}

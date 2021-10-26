package com.bloomcyclecare.cmcc.data.repos.medication;

import com.bloomcyclecare.cmcc.data.models.medication.Medication;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface RWMedicationRepo extends ROMedicationRepo {

  Completable importAll(List<Medication> medications);

  Single<Medication> save(Medication medication);

  Completable delete(Medication medication);
}

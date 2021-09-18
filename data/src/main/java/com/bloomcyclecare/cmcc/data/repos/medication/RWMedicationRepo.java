package com.bloomcyclecare.cmcc.data.repos.medication;

import com.bloomcyclecare.cmcc.data.models.medication.Medication;

import io.reactivex.Single;

public interface RWMedicationRepo extends ROMedicationRepo {

  Single<Medication> save(Medication medication);

}

package com.bloomcyclecare.cmcc.data.repos.prescription;

import androidx.annotation.NonNull;

import com.bloomcyclecare.cmcc.data.models.medication.Prescription;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface RWPrescriptionRepo extends ROPrescriptionRepo {

  Single<Prescription> save(@NonNull Prescription prescription);

  Completable delete(@NonNull Prescription prescription);
}

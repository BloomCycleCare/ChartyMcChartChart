package com.bloomcyclecare.cmcc.data.repos.prescription;

import androidx.annotation.Nullable;

import com.bloomcyclecare.cmcc.data.models.medication.Medication;
import com.bloomcyclecare.cmcc.data.models.medication.Prescription;
import com.google.common.collect.ImmutableList;

import io.reactivex.Observable;

public interface ROPrescriptionRepo {

  Observable<ImmutableList<Prescription>> getPrescriptions(@Nullable Medication medication);
}

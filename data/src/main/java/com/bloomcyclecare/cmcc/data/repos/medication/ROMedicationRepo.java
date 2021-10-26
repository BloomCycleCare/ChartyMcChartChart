package com.bloomcyclecare.cmcc.data.repos.medication;

import com.bloomcyclecare.cmcc.data.models.medication.Medication;

import java.util.List;

import io.reactivex.Flowable;

public interface ROMedicationRepo {

  Flowable<List<Medication>> getAll(boolean includeInactive);
}

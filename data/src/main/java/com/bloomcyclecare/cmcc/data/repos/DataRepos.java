package com.bloomcyclecare.cmcc.data.repos;

import android.app.Application;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.data.models.training.Exercise;
import com.bloomcyclecare.cmcc.data.repos.cycle.RWCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.entry.RWChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.exercise.RWExerciseRepo;
import com.bloomcyclecare.cmcc.data.repos.instructions.RWInstructionsRepo;
import com.bloomcyclecare.cmcc.data.repos.medication.RWMedicationRepo;
import com.bloomcyclecare.cmcc.data.repos.pregnancy.RWPregnancyRepo;
import com.bloomcyclecare.cmcc.data.repos.prescription.RWPrescriptionRepo;
import com.bloomcyclecare.cmcc.data.repos.sticker.RWStickerSelectionRepo;
import com.google.common.collect.Range;

import org.joda.time.LocalDate;

import io.reactivex.Flowable;
import timber.log.Timber;

public interface DataRepos {

  RWInstructionsRepo instructionsRepo(ViewMode viewMode);

  RWCycleRepo cycleRepo(ViewMode viewMode);

  RWCycleRepo cycleRepo(Exercise exercise);

  RWChartEntryRepo entryRepo(ViewMode viewMode);

  RWChartEntryRepo entryRepo(Exercise exercise);

  RWPregnancyRepo pregnancyRepo(ViewMode viewMode);

  RWExerciseRepo exerciseRepo(ViewMode viewMode);

  RWStickerSelectionRepo stickerSelectionRepo(ViewMode viewMode);

  RWStickerSelectionRepo stickerSelectionRepo(Exercise exercise);

  RWMedicationRepo medicationRepo(ViewMode viewMode);

  RWPrescriptionRepo prescriptionRepo(ViewMode viewMode);

  Flowable<Range<LocalDate>> updateStream(int pauseWindowSecs);

  static DataRepos fromApp(Application application) {
    if (application instanceof DataRepos) {
      return (DataRepos) application;
    }
    RuntimeException e = new IllegalStateException();
    Timber.wtf(e);
    throw e;
  }
}

package com.bloomcyclecare.cmcc.backup;

import com.bloomcyclecare.cmcc.ViewMode;
import com.bloomcyclecare.cmcc.apps.charting.ChartingApp;
import com.bloomcyclecare.cmcc.backup.models.AppState;
import com.bloomcyclecare.cmcc.data.models.charting.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.charting.Cycle;
import com.bloomcyclecare.cmcc.data.models.instructions.Instructions;
import com.bloomcyclecare.cmcc.data.repos.cycle.RWCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.entry.RWChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.instructions.RWInstructionsRepo;
import com.bloomcyclecare.cmcc.data.repos.medication.RWMedicationRepo;
import com.bloomcyclecare.cmcc.data.repos.pregnancy.RWPregnancyRepo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Completable;
import timber.log.Timber;

public class AppStateImporter {

  private final RWCycleRepo mCycleRepo;
  private final RWChartEntryRepo mEntryRepo;
  private final RWInstructionsRepo mInstructionsRepo;
  private final RWPregnancyRepo mPregnancyRepo;
  private final RWMedicationRepo mMedicationRepo;

  public AppStateImporter(ChartingApp myApp) {
    mCycleRepo = myApp.cycleRepo(ViewMode.CHARTING);
    mEntryRepo = myApp.entryRepo(ViewMode.CHARTING);
    mInstructionsRepo = myApp.instructionsRepo(ViewMode.CHARTING);
    mPregnancyRepo = myApp.pregnancyRepo(ViewMode.CHARTING);
    mMedicationRepo = myApp.medicationRepo(ViewMode.CHARTING);
  }

  public Completable importAppState(AppState appState) {
    Timber.d("Setting up import");
    List<Completable> actions = new ArrayList<>();
    actions.add(mPregnancyRepo.insertAll(appState.pregnancies)
        .doOnComplete(() -> Timber.d("Done inserting pregnancies"))
        .doOnError(t -> Timber.w(t, "Error inserting pregnancies")));
    actions.add(mMedicationRepo.importAll(appState.medications)
        .doOnComplete(() -> Timber.d("Done inserting medications"))
        .doOnError(t -> Timber.w(t, "Error inserting medications")));
    for (Instructions i : appState.instructions) {
      actions.add(mInstructionsRepo.insertOrUpdate(i));
    }
    actions.add(mInstructionsRepo.commit()
        .doOnComplete(() -> Timber.d("Done inserting instructions"))
        .doOnError(t -> Timber.w(t, "Error inserting instructions")));
    List<Cycle> cycles = new ArrayList<>(appState.cycles);
    Collections.sort(cycles);
    Cycle previousCycle = null;
    for (Cycle c : cycles) {
      if (previousCycle != null) {
        if (!c.startDate.equals(previousCycle.endDate.plusDays(1))) {
          Timber.i("Fixing cycle");
          c.startDate = previousCycle.endDate.plusDays(1);
        }
      }
      actions.add(mCycleRepo.insertOrUpdate(c)
          .doOnError(t -> Timber.w(t, "Error inserting cycle starting: %s", c.startDate)));
      previousCycle = c;
    }
    for (ChartEntry e : appState.entries) {
      actions.add(mEntryRepo.insert(e)
          .doOnError(t -> Timber.w(t, "Error inserting entry: %s", e.entryDate)));
    }
    return Completable.concat(actions)
        .doOnSubscribe(d -> Timber.d("Starting import"))
        .doOnError(t -> Timber.w("Error during import"))
        .doOnComplete(() -> Timber.d("Finished import"));
  }
}

package com.bloomcyclecare.cmcc.data.backup;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.bloomcyclecare.cmcc.data.models.AppState;
import com.bloomcyclecare.cmcc.data.models.ChartEntry;
import com.bloomcyclecare.cmcc.data.repos.cycle.RWCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.entry.RWChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.instructions.RWInstructionsRepo;
import com.bloomcyclecare.cmcc.data.repos.pregnancy.RWPregnancyRepo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Completable;
import timber.log.Timber;

public class AppStateImporter {

  private final RWCycleRepo mCycleRepo;
  private final RWChartEntryRepo mEntryRepo;
  private final RWInstructionsRepo mInstructionsRepo;
  private final RWPregnancyRepo mPregnancyRepo;

  public AppStateImporter(MyApplication myApp) {
    mCycleRepo = myApp.cycleRepo();
    mEntryRepo = myApp.entryRepo();
    mInstructionsRepo = myApp.instructionsRepo();
    mPregnancyRepo = myApp.pregnancyRepo();
  }

  public Completable importAppState(AppState appState) {
    Set<Completable> actions = new HashSet<>();
    actions.add(mPregnancyRepo.insertAll(appState.pregnancies));
    for (Instructions i : appState.instructions) {
      actions.add(mInstructionsRepo.insertOrUpdate(i));
    }
    actions.add(mInstructionsRepo.commit());
    List<Cycle> cycles = new ArrayList<>(appState.cycles);
    Collections.sort(cycles);
    Cycle previousCycle = null;
    for (Cycle c : cycles) {
      if (previousCycle != null) {
        if (!c.startDate.equals(previousCycle.endDate.plusDays(1))) {
          Timber.w("Fixing cycle");
          c.startDate = previousCycle.endDate.plusDays(1);
        }
      }
      actions.add(mCycleRepo.insertOrUpdate(c));
      previousCycle = c;
    }
    for (ChartEntry e : appState.entries) {
      actions.add(mEntryRepo.insert(e));
    }
    return Completable.concat(actions);
  }
}

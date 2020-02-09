package com.bloomcyclecare.cmcc.data.backup;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.bloomcyclecare.cmcc.data.models.AppState;
import com.bloomcyclecare.cmcc.data.repos.ChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.CycleRepo;
import com.bloomcyclecare.cmcc.data.repos.InstructionsRepo;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.Completable;

public class AppStateImporter {

  private final CycleRepo mCycleRepo;
  private final ChartEntryRepo mEntryRepo;
  private final InstructionsRepo mInstructionsRepo;

  public AppStateImporter(MyApplication myApp) {
    mCycleRepo = new CycleRepo(myApp.db());
    mEntryRepo = new ChartEntryRepo(myApp.db());
    mInstructionsRepo = myApp.instructionsRepo();
  }

  public Completable importAppState(AppState appState) {
    Set<Completable> actions = new HashSet<>();
    for (Instructions i : appState.instructions) {
      actions.add(mInstructionsRepo.insertOrUpdate(i));
    }
    actions.add(mInstructionsRepo.commit());
    for (AppState.CycleData d : appState.cycles) {
      actions.add(mCycleRepo.insertOrUpdate(d.cycle));
      actions.add(mEntryRepo.insertAll(d.entries));
    }
    return Completable.merge(actions);
  }
}

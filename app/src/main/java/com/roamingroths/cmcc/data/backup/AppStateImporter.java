package com.roamingroths.cmcc.data.backup;

import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.entities.Instructions;
import com.roamingroths.cmcc.data.models.AppState;
import com.roamingroths.cmcc.data.repos.ChartEntryRepo;
import com.roamingroths.cmcc.data.repos.CycleRepo;
import com.roamingroths.cmcc.data.repos.InstructionsRepo;

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
    for (AppState.CycleData d : appState.cycles) {
      actions.add(mCycleRepo.insertOrUpdate(d.cycle));
      actions.add(mEntryRepo.insertAll(d.entries));
    }
    return Completable.merge(actions);
  }
}

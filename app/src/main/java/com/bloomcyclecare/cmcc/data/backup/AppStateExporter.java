package com.bloomcyclecare.cmcc.data.backup;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.data.models.AppState;
import com.bloomcyclecare.cmcc.data.repos.InstructionsRepo;
import com.bloomcyclecare.cmcc.data.repos.PregnancyRepo;
import com.bloomcyclecare.cmcc.data.repos.cycle.RWCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.entry.RWChartEntryRepo;

import io.reactivex.Single;

public class AppStateExporter {

  private final RWCycleRepo mCycleRepo;
  private final RWChartEntryRepo mEntryRepo;
  private final InstructionsRepo mInstructionsRepo;
  private final PregnancyRepo mPregnancyRepo;

  public static AppStateExporter forApp(MyApplication myApp) {
    return new AppStateExporter(myApp);
  }

  public AppStateExporter(MyApplication myApp) {
    mCycleRepo = myApp.cycleRepo();
    mEntryRepo = myApp.entryRepo();
    mInstructionsRepo = myApp.instructionsRepo();
    mPregnancyRepo = myApp.pregnancyRepo();
  }

  public Single<AppState> export() {
    return Single.zip(
        mCycleRepo.getStream().firstOrError(),
        mEntryRepo.getAllEntries(),
        mInstructionsRepo.getAll().firstOrError(),
        mPregnancyRepo.getAll().firstOrError(),
        (cycles, entries, instructions, pregnancies) -> new AppState(
            cycles, entries,  null, instructions, pregnancies));
  }}

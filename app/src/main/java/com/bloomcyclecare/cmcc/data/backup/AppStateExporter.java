package com.bloomcyclecare.cmcc.data.backup;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.data.models.AppState;
import com.bloomcyclecare.cmcc.data.repos.ChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.CycleRepo;
import com.bloomcyclecare.cmcc.data.repos.InstructionsRepo;
import com.bloomcyclecare.cmcc.data.repos.PregnancyRepo;

import io.reactivex.Single;

public class AppStateExporter {

  private final CycleRepo mCycleRepo;
  private final ChartEntryRepo mEntryRepo;
  private final InstructionsRepo mInstructionsRepo;
  private final PregnancyRepo mPregnancyRepo;

  public static AppStateExporter forApp(MyApplication myApp) {
    return new AppStateExporter(myApp);
  }

  public AppStateExporter(MyApplication myApp) {
    mCycleRepo = new CycleRepo(myApp.db());
    mEntryRepo = new ChartEntryRepo(myApp.db());
    mInstructionsRepo = myApp.instructionsRepo();
    mPregnancyRepo = myApp.pregnancyRepo();
  }

  public Single<AppState> export() {
    return Single.zip(
        mCycleRepo.getStream().firstOrError(),
        mEntryRepo.getAllEntries(),
        mInstructionsRepo.getAll().firstOrError(),
        mPregnancyRepo.getAll(),
        (cycles, entries, instructions, pregnancies) -> new AppState(
            cycles, entries,  null, instructions, pregnancies));
  }}

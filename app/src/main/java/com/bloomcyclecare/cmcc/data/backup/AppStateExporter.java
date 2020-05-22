package com.bloomcyclecare.cmcc.data.backup;

import com.bloomcyclecare.cmcc.application.MyApplication;
import com.bloomcyclecare.cmcc.application.ViewMode;
import com.bloomcyclecare.cmcc.data.models.AppState;
import com.bloomcyclecare.cmcc.data.repos.cycle.ROCycleRepo;
import com.bloomcyclecare.cmcc.data.repos.entry.ROChartEntryRepo;
import com.bloomcyclecare.cmcc.data.repos.instructions.ROInstructionsRepo;
import com.bloomcyclecare.cmcc.data.repos.pregnancy.ROPregnancyRepo;

import io.reactivex.Single;

public class AppStateExporter {

  private final ROCycleRepo mCycleRepo;
  private final ROChartEntryRepo mEntryRepo;
  private final ROInstructionsRepo mInstructionsRepo;
  private final ROPregnancyRepo mPregnancyRepo;

  public static AppStateExporter forApp(MyApplication myApp) {
    return new AppStateExporter(myApp);
  }

  public AppStateExporter(MyApplication myApp) {
    mCycleRepo = myApp.cycleRepo(ViewMode.CHARTING);
    mEntryRepo = myApp.entryRepo(ViewMode.CHARTING);
    mInstructionsRepo = myApp.instructionsRepo(ViewMode.CHARTING);
    mPregnancyRepo = myApp.pregnancyRepo(ViewMode.CHARTING);
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

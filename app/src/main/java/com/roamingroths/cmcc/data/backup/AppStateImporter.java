package com.roamingroths.cmcc.data.backup;

import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.models.AppState;
import com.roamingroths.cmcc.data.repos.ChartEntryRepo;
import com.roamingroths.cmcc.data.repos.CycleRepo;

import io.reactivex.Completable;
import io.reactivex.Observable;

public class AppStateImporter {

  private final CycleRepo mCycleRepo;
  private final ChartEntryRepo mEntryRepo;

  public AppStateImporter(MyApplication myApp) {
    mCycleRepo = new CycleRepo(myApp.db());
    mEntryRepo = new ChartEntryRepo(myApp.db());
  }

  public Completable importAppState(AppState appState) {
    return Observable
        .fromIterable(appState.cycles)
        .flatMapCompletable(cycleData -> Completable.mergeArray(
            mCycleRepo.insertOrUpdate(cycleData.cycle),
            mEntryRepo.insertAll(cycleData.entries)));
  }
}

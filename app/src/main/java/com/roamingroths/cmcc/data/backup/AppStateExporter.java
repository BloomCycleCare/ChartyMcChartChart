package com.roamingroths.cmcc.data.backup;

import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.models.AppState;
import com.roamingroths.cmcc.data.repos.ChartEntryRepo;
import com.roamingroths.cmcc.data.repos.CycleRepo;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;

public class AppStateExporter {

  private final CycleRepo mCycleRepo;
  private final ChartEntryRepo mEntryRepo;

  public AppStateExporter(MyApplication myApp) {
    mCycleRepo = new CycleRepo(myApp.db());
    mEntryRepo = new ChartEntryRepo(myApp.db());
  }

  public Single<AppState> export() {
    return mCycleRepo.getStream()
        .firstOrError()
        .flatMapObservable(Observable::fromIterable)
        .flatMapSingle(cycle -> mEntryRepo
            .getStream(Flowable.just(cycle))
            .firstOrError()
            .map(entries -> new AppState.CycleData(cycle, entries))
        )
        .toList()
        .map(cycleDatas -> new AppState(cycleDatas, null));
  }
}

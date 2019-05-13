package com.roamingroths.cmcc.providers;

import com.roamingroths.cmcc.data.db.AppDatabase;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.data.entities.ObservationEntry;
import com.roamingroths.cmcc.data.entities.SymptomEntry;
import com.roamingroths.cmcc.data.entities.WellnessEntry;
import com.roamingroths.cmcc.data.models.ChartEntry;
import com.roamingroths.cmcc.data.repos.ChartEntryRepo;
import com.roamingroths.cmcc.utils.DateUtil;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;

/**
 * Created by parkeroth on 11/18/17.
 */

public class ChartEntryProvider {

  private static boolean DEBUG = true;
  private static String TAG = ChartEntryProvider.class.getSimpleName();

  private final ChartEntryCache mCache;
  private final ChartEntryRepo mRepo;

  public ChartEntryProvider(AppDatabase localDB) {
    mCache = new ChartEntryCache();
    mRepo = new ChartEntryRepo(localDB);
  }

  public Observable<ChartEntry> getEntries(Cycle cycle) {
    return mRepo
        .getStream(Flowable.just(cycle))
        .firstOrError()
        .flatMapObservable(Observable::fromIterable);
  }

  @Deprecated
  public Completable putEntry(Cycle cycle, ChartEntry chartEntry) {
    return mRepo.insert(chartEntry);
  }

  public Completable createEmptyEntries(Cycle cycle) {
    return Observable
        .fromIterable(DateUtil.daysBetween(cycle.startDate, cycle.endDate))
        .map(date -> new ChartEntry(
              date,
              ObservationEntry.emptyEntry(date),
              WellnessEntry.emptyEntry(date),
              SymptomEntry.emptyEntry(date)))
        .flatMapCompletable(mRepo::insert);
  }
}

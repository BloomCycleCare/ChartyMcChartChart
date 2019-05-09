package com.roamingroths.cmcc.providers;

import android.util.Log;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.data.db.AppDatabase;
import com.roamingroths.cmcc.data.db.ObservationEntryDao;
import com.roamingroths.cmcc.logic.AppState;
import com.roamingroths.cmcc.data.models.ChartEntry;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.utils.DateUtil;
import com.roamingroths.cmcc.utils.UpdateHandle;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

import durdinapps.rxfirebase2.RxFirebaseChildEvent;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by parkeroth on 11/18/17.
 */

public class ChartEntryProvider {

  private static boolean DEBUG = true;
  private static String TAG = ChartEntryProvider.class.getSimpleName();

  private final ChartEntryCache mCache;
  private final ChartEntryStore mStore;
  private final ObservationEntryDao mDao;

  public ChartEntryProvider(FirebaseDatabase db, AppDatabase localDB, CryptoUtil mCryptoUtil) {
    this.mStore = new ChartEntryStore(db, mCryptoUtil);
    this.mCache = new ChartEntryCache();
    mDao = localDB.observationEntryDao();
  }

  public Completable initCache(Observable<Cycle> cycles, int numCycles) {
    Completable fillCache = cycles
        .observeOn(Schedulers.io())
        .sorted((o1, o2) -> o2.startDate.compareTo(o1.startDate))
        .take(numCycles)
        .toList()
        .flatMapCompletable(new Function<List<Cycle>, CompletableSource>() {
          @Override
          public CompletableSource apply(List<Cycle> cycles) throws Exception {
            List<Completable> completables = new ArrayList<>(cycles.size());
            for (Cycle cycle : cycles) {
              completables.add(fillCache(cycle));
            }
            return Completable.merge(completables);
          }
        });
    Completable fillRoom = cycles
        .flatMap(cycle -> mStore.getEntries(cycle, Predicates.alwaysTrue()))
        .map(chartEntry -> chartEntry.observationEntry)
        .flatMapCompletable(observationEntry -> mDao
            .delete(observationEntry)
            .andThen(mDao.insert(observationEntry)));
    return Completable.mergeArray(fillCache, fillRoom);
  }

  public Completable fillCache(final Cycle cycle) {
    if (DEBUG) Log.v(TAG, "Fill entry cache: " + cycle);
    return getEntries(cycle).ignoreElements().doOnComplete(new Action() {
      @Override
      public void run() throws Exception {
        if (DEBUG) Log.v(TAG, "Done filling entry cache: " + cycle);
      }
    });
  }

  public Flowable<RxFirebaseChildEvent<ChartEntry>> entryStream(Cycle cycle) {
    // TODO: use cache?
    return mStore.entryStream(cycle);
  }

  public Observable<ChartEntry> getEntries(Cycle cycle) {
    return getEntries(cycle, Predicates.<LocalDate>alwaysTrue());
  }

  public Single<UpdateHandle> putCycleData(AppState.CycleData cycleData) {
    return mStore.putEntriesDeferred(cycleData.cycle, Observable.fromIterable(cycleData.entries));
  }

  public Observable<ChartEntry> getEntries(Cycle cycle, Predicate<LocalDate> datePredicate) {
    return Observable.fromIterable(DateUtil.daysBetween(cycle.startDate, cycle.endDate))
        .filter(datePredicate::apply)
        .toList()
        .flatMapObservable(entryDates -> {
          List<ChartEntry> cachedEntries = mCache.getEntries(entryDates);
          if (DEBUG) Log.v(TAG, "Fetch values for: " + cycle);
          if (entryDates.size() == cachedEntries.size()) {
            if (DEBUG) Log.v(TAG, "Return cached values for: " + cycle);
            return Observable.fromIterable(cachedEntries);
          }
          Log.i(TAG, "Cache miss for cycle:" + cycle.id);
          return mStore.getEntries(cycle, datePredicate).doOnEach(mCache.fill());
        });
  }

  @Deprecated
  public Completable putEntry(Cycle cycle, ChartEntry chartEntry) {
    return mStore.putEntry(cycle, chartEntry).doOnComplete(mCache.putEntry(chartEntry));
  }

  public Single<UpdateHandle> createEmptyEntries(Cycle cycle) {
    Observable<LocalDate> daysToAdd = Observable.fromIterable(DateUtil.daysBetween(cycle.startDate, cycle.endDate));
    return storeEntries(cycle, daysToAdd);
  }

  private Single<UpdateHandle> storeEntries(Cycle cycle, Observable<LocalDate> dates) {
    return dates
        .map(date -> mStore.createEmptyEntry(cycle, date))
        .toList()
        .flatMap(chartEntries -> {
          List<Single<UpdateHandle>> handles = new ArrayList<>();
          for (final ChartEntry entry : chartEntries) {
            handles.add(mStore.putEntryDeferred(cycle, entry).map(updateHandle -> {
              updateHandle.actions.add(mCache.putEntry(entry));
              return updateHandle;
            }));
          }
          return Single.concat(handles).collectInto(mStore.newHandle(), UpdateHandle.collector());
        });
  }

  public Single<UpdateHandle> maybeAddNewEntriesDeferred(final Cycle cycle) {
    Observable<LocalDate> daysToAdd = getEntries(cycle)
        .toList()
        .flatMapMaybe(findMostRecent())
        // use tomorrow if no entries exist
        .switchIfEmpty(Maybe.just(cycle.startDate.minusDays(1)))
        // emit days between now and most recent entry
        .flatMapObservable(new Function<LocalDate, ObservableSource<LocalDate>>() {
          @Override
          public ObservableSource<LocalDate> apply(LocalDate lastEntryDate) throws Exception {
            return Observable.fromIterable(DateUtil.daysBetween(lastEntryDate.plusDays(1), LocalDate.now()));
          }
        });
    return storeEntries(cycle, daysToAdd);
  }

  public Single<UpdateHandle> dropEntries(final Cycle cycle) {
    return dropEntries(cycle, Predicates.<LocalDate>alwaysTrue());
  }

  public Single<UpdateHandle> dropEntries(final Cycle cycle, Predicate<LocalDate> datePredicate) {
    return getEntries(cycle, datePredicate).toList().flatMap(new Function<List<ChartEntry>, SingleSource<? extends UpdateHandle>>() {
      @Override
      public SingleSource<? extends UpdateHandle> apply(List<ChartEntry> chartEntries) throws Exception {
        UpdateHandle handle = mStore.newHandle();
        for (String entryPath : mStore.getDbPaths(cycle, chartEntries)) {
          handle.updates.put(entryPath, null);
        }
        handle.actions.add(mCache.dropEntries(chartEntries));
        return Single.just(handle);
      }
    });
  }

  public Single<UpdateHandle> copyEntries(
      final Cycle fromCycle,
      final Cycle toCycle,
      Predicate<LocalDate> datePredicate) {
    if (DEBUG) Log.v(TAG, "Copy entries from " + fromCycle.id + " to " + toCycle.id);
    Observable<ChartEntry> entriesToPut = getEntries(fromCycle, datePredicate).map(mStore.swapKeys(toCycle)).cache();
    Single<UpdateHandle> updates = mStore.putEntriesDeferred(toCycle, entriesToPut);
    return Observable.zip(entriesToPut, updates.toObservable(), new BiFunction<ChartEntry, UpdateHandle, UpdateHandle>() {
      @Override
      public UpdateHandle apply(ChartEntry chartEntry, UpdateHandle updateHandle) throws Exception {
        updateHandle.actions.add(mCache.putEntry(chartEntry));
        return updateHandle;
      }
    }).collectInto(mStore.newHandle(), UpdateHandle.collector());
  }

  private Function<List<ChartEntry>, MaybeSource<LocalDate>> findMostRecent() {
    return new Function<List<ChartEntry>, MaybeSource<LocalDate>>() {
      @Override
      public MaybeSource<LocalDate> apply(List<ChartEntry> entries) throws Exception {
        if (entries.isEmpty()) {
          return Maybe.empty();
        }
        LocalDate lastEntryDate = null;
        for (ChartEntry entry : entries) {
          if (lastEntryDate == null || lastEntryDate.isBefore(entry.entryDate)) {
            lastEntryDate = entry.entryDate;
          }
        }
        return Maybe.just(lastEntryDate);
      }
    };
  }
}

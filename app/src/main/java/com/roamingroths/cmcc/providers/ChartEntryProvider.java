package com.roamingroths.cmcc.providers;

import android.util.Log;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.utils.UpdateHandle;
import com.roamingroths.cmcc.logic.chart.ChartEntry;
import com.roamingroths.cmcc.logic.chart.Cycle;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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

  public ChartEntryProvider(FirebaseDatabase db, CryptoUtil mCryptoUtil) {
    this.mStore = new ChartEntryStore(db, mCryptoUtil);
    this.mCache = new ChartEntryCache();
  }

  public Completable initCache(Observable<Cycle> cycles, int numCycles) {
    return cycles
        .observeOn(Schedulers.io())
        .sorted(new Comparator<Cycle>() {
          @Override
          public int compare(Cycle o1, Cycle o2) {
            return o2.startDate.compareTo(o1.startDate);
          }
        })
        .take(numCycles)
        .flatMapCompletable(new Function<Cycle, CompletableSource>() {
          @Override
          public CompletableSource apply(Cycle cycle) throws Exception {
            return fillCache(cycle);
          }
        });
  }

  public Completable fillCache(final Cycle cycle) {
    if (DEBUG) Log.v(TAG, "Fill entry cache: " + cycle);
    return getEntries(cycle).ignoreElements();
  }

  public Flowable<RxFirebaseChildEvent<ChartEntry>> entryStream(Cycle cycle) {
    // TODO: use cache?
    return mStore.entryStream(cycle);
  }

  public Observable<ChartEntry> getEntries(Cycle cycle) {
    return getEntries(cycle, Predicates.<LocalDate>alwaysTrue());
  }

  public Observable<ChartEntry> getEntries(Cycle cycle, Predicate<LocalDate> datePredicate) {
    Collection<LocalDate> entryDates =
        Collections2.filter(DateUtil.daysBetween(cycle.startDate, cycle.endDate), datePredicate);
    List<ChartEntry> cachedEntries = mCache.getEntries(entryDates);
    if (entryDates.size() == cachedEntries.size()) {
      if (DEBUG) Log.v(TAG, "Return cached values for: " + cycle);
      return Observable.fromIterable(cachedEntries);
    }
    if (DEBUG) Log.v(TAG, "Fetch values for: " + cycle);
    return mStore.getEntries(cycle, datePredicate).doOnEach(mCache.fill());
  }

  @Deprecated
  public Completable putEntry(Cycle cycle, ChartEntry chartEntry) {
    return mStore.putEntry(cycle, chartEntry).doOnComplete(mCache.putEntry(chartEntry));
  }

  public Single<UpdateHandle> maybeAddNewEntriesDeferred(final Cycle cycle) {
    return getEntries(cycle)
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
        })
        .map(new Function<LocalDate, ChartEntry>() {
          @Override
          public ChartEntry apply(LocalDate localDate) throws Exception {
            return mStore.createEmptyEntry(cycle, localDate);
          }
        })
        .toList()
        .flatMap(new Function<List<ChartEntry>, SingleSource<? extends UpdateHandle>>() {
          @Override
          public SingleSource<UpdateHandle> apply(List<ChartEntry> chartEntries) throws Exception {
            List<Single<UpdateHandle>> handles = new ArrayList<>();
            for (final ChartEntry entry : chartEntries) {
              handles.add(mStore.putEntryDeferred(cycle, entry).map(new Function<UpdateHandle, UpdateHandle>() {
                @Override
                public UpdateHandle apply(UpdateHandle updateHandle) throws Exception {
                  updateHandle.actions.add(mCache.putEntry(entry));
                  return updateHandle;
                }
              }));
            }
            return Single.concat(handles).collectInto(mStore.newHandle(), UpdateHandle.collector());
          }
        });
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
      final Predicate<LocalDate> datePredicate) {
    if (DEBUG) Log.v(TAG, "Copy entries from " + fromCycle.id + " to " + toCycle.id);
    Observable<ChartEntry> entriesToPut = getEntries(fromCycle, datePredicate).map(mStore.swapKeys(toCycle)).cache();
    Observable<UpdateHandle> updates = mStore.putEntriesDeferred(toCycle, entriesToPut);
    return Observable.zip(entriesToPut, updates, new BiFunction<ChartEntry, UpdateHandle, UpdateHandle>() {
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

package com.roamingroths.cmcc.data;

import android.util.Log;

import com.google.common.base.Predicate;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import java.util.Comparator;
import java.util.List;

import durdinapps.rxfirebase2.RxFirebaseChildEvent;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Notification;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Consumer;
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
    List<LocalDate> entryDates = DateUtil.daysBetween(cycle.startDate, cycle.endDate);
    List<ChartEntry> cachedEntries = mCache.getEntries(entryDates);
    if (entryDates.size() == cachedEntries.size()) {
      if (DEBUG) Log.v(TAG, "Return cached values for: " + cycle);
      return Observable.fromIterable(cachedEntries);
    }
    if (DEBUG) Log.v(TAG, "Fetch values for: " + cycle);
    return mStore.getEntries(cycle).doOnEach(mCache.fill());
  }

  @Deprecated
  public Completable putEntry(Cycle cycle, ChartEntry chartEntry) {
    return mStore.putEntry(cycle, chartEntry).doOnComplete(mCache.putEntry(chartEntry));
  }

  public Completable maybeAddNewEntries(final Cycle cycle) {
    return getEntries(cycle)
        .toList()
        .flatMapMaybe(findMostRecent())
        // use tomorrow if no entries exist
        .switchIfEmpty(Maybe.just(cycle.startDate.minusDays(1)))
        // emit days between now and most recent entry
        .flatMapObservable(new Function<LocalDate, ObservableSource<LocalDate>>() {
          @Override
          public ObservableSource<LocalDate> apply(final LocalDate endDate) throws Exception {
            return Observable.create(new ObservableOnSubscribe<LocalDate>() {
              @Override
              public void subscribe(ObservableEmitter<LocalDate> e) throws Exception {
                LocalDate today = DateUtil.now();
                for (LocalDate date = today; date.isAfter(endDate); date = date.minusDays(1)) {
                  e.onNext(date);
                }
                e.onComplete();
              }
            });
          }
        })
        // store the entries
        .flatMapCompletable(new Function<LocalDate, CompletableSource>() {
          @Override
          public CompletableSource apply(LocalDate localDate) throws Exception {
            return mStore.putEmptyEntry(cycle, localDate)
                .toObservable().doOnEach(mCache.fill()).ignoreElements();
          }
        });
  }

  public Completable moveEntries(
      final Cycle fromCycle,
      final Cycle toCycle,
      final Predicate<LocalDate> datePredicate) {
    if (DEBUG) Log.v(TAG, "Move entries from " + fromCycle.id + " to " + toCycle.id);
    Observable<ChartEntry> entriesToMove = getEntries(fromCycle).doOnEach(new Consumer<Notification<ChartEntry>>() {
      @Override
      public void accept(Notification<ChartEntry> chartEntryNotification) throws Exception {
        return;
      }
    }).filter(new io.reactivex.functions.Predicate<ChartEntry>() {
      @Override
      public boolean test(ChartEntry chartEntry) throws Exception {
        return datePredicate.apply(chartEntry.entryDate);
      }
    }).cache();
    return mStore.moveEntries(fromCycle, toCycle, entriesToMove).doOnEach(mCache.fill()).ignoreElements();
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

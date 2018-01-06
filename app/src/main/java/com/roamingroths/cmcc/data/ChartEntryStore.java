package com.roamingroths.cmcc.data;

import android.util.Log;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.Entry;
import com.roamingroths.cmcc.logic.ObservationEntry;
import com.roamingroths.cmcc.logic.SymptomEntry;
import com.roamingroths.cmcc.logic.WellnessEntry;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;
import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.SecretKey;

import durdinapps.rxfirebase2.RxFirebaseChildEvent;
import durdinapps.rxfirebase2.RxFirebaseDatabase;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;
import io.reactivex.functions.Function4;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by parkeroth on 12/31/17.
 */

public class ChartEntryStore {

  private static boolean DEBUG = true;
  private static String TAG = ChartEntryStore.class.getSimpleName();

  private final FirebaseDatabase mDB;
  private final ObservationEntryProvider mObservationEntryProvider;
  private final WellnessEntryProvider mWellnessEntryProvider;
  private final SymptomEntryProvider mSymptomEntryProvider;

  public ChartEntryStore(FirebaseDatabase db, CryptoUtil cryptoUtil) {
    mDB = db;
    mObservationEntryProvider = new ObservationEntryProvider(cryptoUtil);
    mWellnessEntryProvider = new WellnessEntryProvider(cryptoUtil);
    mSymptomEntryProvider = new SymptomEntryProvider(cryptoUtil);
  }

  public Observable<ChartEntry> getEntries(Cycle cycle, Predicate<LocalDate> datePredicate) {
    return RxFirebaseDatabase.observeSingleValueEvent(reference(cycle))
        .observeOn(Schedulers.computation())
        .flatMapObservable(new Function<DataSnapshot, ObservableSource<DataSnapshot>>() {
          @Override
          public ObservableSource<DataSnapshot> apply(final DataSnapshot dataSnapshot) throws Exception {
            return Observable.fromIterable(dataSnapshot.getChildren());
          }
        })
        .flatMap(snapshotToEntry(cycle, datePredicate));
  }

  public Flowable<RxFirebaseChildEvent<ChartEntry>> entryStream(final Cycle cycle) {
    return RxFirebaseDatabase.observeChildEvent(reference(cycle))
        .observeOn(Schedulers.computation())
        .flatMap(new Function<RxFirebaseChildEvent<DataSnapshot>, Publisher<RxFirebaseChildEvent<ChartEntry>>>() {
          @Override
          public Publisher<RxFirebaseChildEvent<ChartEntry>> apply(final RxFirebaseChildEvent<DataSnapshot> childEvent) throws Exception {
            return snapshotToEntry(cycle).apply(childEvent.getValue())
                .map(new Function<ChartEntry, RxFirebaseChildEvent<ChartEntry>>() {
                  @Override
                  public RxFirebaseChildEvent<ChartEntry> apply(ChartEntry chartEntry) throws Exception {
                    return new RxFirebaseChildEvent<>(childEvent.getKey(), chartEntry, childEvent.getEventType());
                  }
                }).toFlowable(BackpressureStrategy.BUFFER);
          }
        });
  }

  public List<String> getDbPaths(Cycle cycle, List<ChartEntry> entries) {
    List<String> paths = new ArrayList<>();
    for (ChartEntry entry : entries) {
      paths.add(String.format("/entries/%s/%s", cycle.id, DateUtil.toWireStr(entry.entryDate)));
    }
    return paths;
  }

  public Function<ChartEntry, ChartEntry> swapKeys(final Cycle toCycle) {
    return new Function<ChartEntry, ChartEntry>() {
      @Override
      public ChartEntry apply(ChartEntry chartEntry) throws Exception {
        chartEntry.observationEntry.swapKey(mObservationEntryProvider.getKey(toCycle.keys));
        chartEntry.wellnessEntry.swapKey(mWellnessEntryProvider.getKey(toCycle.keys));
        chartEntry.symptomEntry.swapKey(mSymptomEntryProvider.getKey(toCycle.keys));
        return chartEntry;
      }
    };
  }



  public Observable<UpdateHandle> putEntriesDeferred(final Cycle toCycle, Observable<ChartEntry> entries) {
    return entries.flatMap(new Function<ChartEntry, ObservableSource<UpdateHandle>>() {
      @Override
      public ObservableSource<UpdateHandle> apply(ChartEntry chartEntry) throws Exception {
        return putEntryDeferred(toCycle, chartEntry).toObservable();
      }
    });
  }

  public Observable<ChartEntry> moveEntries(final Cycle fromCycle, final Cycle toCycle, Observable<ChartEntry> entries) {
    return entries
        .map(new Function<ChartEntry, ChartEntry>() {
          @Override
          public ChartEntry apply(ChartEntry chartEntry) throws Exception {
            chartEntry.observationEntry.swapKey(mObservationEntryProvider.getKey(toCycle.keys));
            chartEntry.wellnessEntry.swapKey(mWellnessEntryProvider.getKey(toCycle.keys));
            chartEntry.symptomEntry.swapKey(mSymptomEntryProvider.getKey(toCycle.keys));
            return chartEntry;
          }
        })
        .toList()
        .flatMapObservable(new Function<List<ChartEntry>, Observable<ChartEntry>>() {
          @Override
          public Observable<ChartEntry> apply(final List<ChartEntry> chartEntries) throws Exception {
            Set<Completable> puts = new HashSet<>();
            for (ChartEntry entry : chartEntries) {
              if (DEBUG) Log.v(TAG, "Moving " + entry.entryDate + " from " + fromCycle.id + " to " + toCycle.id);
              puts.add(putEntry(toCycle, entry).andThen(deleteEntry(fromCycle, entry)));
            }
            return Completable.merge(puts).andThen(Observable.fromIterable(chartEntries));
          }
        });
  }

  public Completable deleteEntry(Cycle cycle, ChartEntry entry) {
    if (DEBUG) Log.v(TAG, "Delete " + entry.entryDate + " from " + cycle.id);
    return RxFirebaseDatabase.removeValue(reference(cycle, entry.entryDate)).observeOn(Schedulers.io()).doOnComplete(new Action() {
      @Override
      public void run() throws Exception {
        Log.i(TAG, "Delete done");
      }
    });
  }

  public Single<ChartEntry> putEmptyEntry(Cycle cycle, LocalDate date) {
    ChartEntry entry = createEmpty(date, cycle.keys);
    if (cycle.startDate.isEqual(date)) {
      entry.observationEntry.firstDay = true;
    }
    return putEntry(cycle, entry).andThen(Single.just(entry));
  }

  public Single<UpdateHandle> putEntryDeferred(final Cycle cycle, final ChartEntry chartEntry) {
    List<Single<EncryptedEntry>> encryptedEntries = new ArrayList<>();
    encryptedEntries.add(mObservationEntryProvider.encryptEntry(chartEntry.observationEntry));
    encryptedEntries.add(mWellnessEntryProvider.encryptEntry(chartEntry.wellnessEntry));
    encryptedEntries.add(mSymptomEntryProvider.encryptEntry(chartEntry.symptomEntry));
    return Single.concat(encryptedEntries).toList().map(new Function<List<EncryptedEntry>, UpdateHandle>() {
      @Override
      public UpdateHandle apply(List<EncryptedEntry> encryptedEntries) throws Exception {
        String basePath = String.format("/entries/%s/%s/", cycle.id, DateUtil.toWireStr(chartEntry.entryDate));
        UpdateHandle handle = new UpdateHandle();
        for (EncryptedEntry entry : encryptedEntries) {
          handle.updates.put(basePath + entry.childId, entry.encryptedEntry);
        }
        return handle;
      }
    });
  }

  public Completable putEntry(final Cycle cycle, final ChartEntry chartEntry) {
    Set<Single<EncryptedEntry>> encryptedEntries = new HashSet<>();
    encryptedEntries.add(mObservationEntryProvider.encryptEntry(chartEntry.observationEntry));
    encryptedEntries.add(mWellnessEntryProvider.encryptEntry(chartEntry.wellnessEntry));
    encryptedEntries.add(mSymptomEntryProvider.encryptEntry(chartEntry.symptomEntry));

    return Single.merge(encryptedEntries)
        .toList()
        .flatMapCompletable(new Function<List<EncryptedEntry>, CompletableSource>() {
          @Override
          public CompletableSource apply(List<EncryptedEntry> encryptedEntries) throws Exception {
            Map<String, Object> updates = new HashMap<>();
            for (EncryptedEntry entry : encryptedEntries) {
              updates.put(entry.childId, entry.encryptedEntry);
            }
            if (DEBUG) Log.v(TAG, "Put " + chartEntry.entryDate + " to " + cycle);
            return RxFirebaseDatabase.updateChildren(reference(cycle, chartEntry.entryDate), updates);
          }
        });
  }

  private Function<DataSnapshot, Observable<ChartEntry>> snapshotToEntry(Cycle cycle) {
    return snapshotToEntry(cycle, Predicates.<LocalDate>alwaysTrue());
  }

  private Function<DataSnapshot, Observable<ChartEntry>> snapshotToEntry(final Cycle cycle, final Predicate<LocalDate> datePredicate) {
    return new Function<DataSnapshot, Observable<ChartEntry>>() {
      @Override
      public Observable<ChartEntry> apply(DataSnapshot snapshot) {
        LocalDate entryDate = DateUtil.fromWireStr(snapshot.getKey());
        if (!datePredicate.apply(entryDate)) {
          return Observable.empty();
        }
        Single<ObservationEntry> observation =
            mObservationEntryProvider.decryptEntry(snapshot, cycle.keys);
        Single<WellnessEntry> wellness =
            mWellnessEntryProvider.decryptEntry(snapshot, cycle.keys);
        Single<SymptomEntry> symptom =
            mSymptomEntryProvider.decryptEntry(snapshot, cycle.keys);
        return Single.zip(Single.just(entryDate), observation, wellness, symptom, new Function4<LocalDate, ObservationEntry, WellnessEntry, SymptomEntry, ChartEntry>() {
          @Override
          public ChartEntry apply(LocalDate localDate, ObservationEntry observationEntry, WellnessEntry wellnessEntry, SymptomEntry symptomEntry) throws Exception {
            return new ChartEntry(localDate, observationEntry, wellnessEntry, symptomEntry);
          }
        }).toObservable();
      }
    };
  }

  private ChartEntry createEmpty(LocalDate entryDate, Cycle.Keys keys) {
    ObservationEntry observation = mObservationEntryProvider.createEmpty(entryDate, keys);
    WellnessEntry wellness = mWellnessEntryProvider.createEmpty(entryDate, keys);
    SymptomEntry symptomEntry = mSymptomEntryProvider.createEmpty(entryDate, keys);

    return new ChartEntry(entryDate, observation, wellness, symptomEntry);
  }

  private final DatabaseReference reference(Cycle cycle) {
    return mDB.getReference("entries").child(cycle.id);
  }

  private final DatabaseReference reference(Cycle cycle, LocalDate entryDate) {
    return reference(cycle).child(DateUtil.toWireStr(entryDate));
  }

  private static class EncryptedEntry {
    public final String childId;
    public final String encryptedEntry;

    public EncryptedEntry(String childId, String encryptedEntry) {
      this.childId = childId;
      this.encryptedEntry = encryptedEntry;
    }
  }

  private static abstract class SubEntryProvider<E extends Entry> {
    private final Class<E> mClazz;
    private final String mChildId;
    private final CryptoUtil mCryptoUtil;

    SubEntryProvider(Class<E> clazz, String childId, CryptoUtil cryptoUtil) {
      mClazz = clazz;
      mChildId = childId;
      mCryptoUtil = cryptoUtil;
    }

    abstract E createEmpty(LocalDate entryDate, SecretKey key);

    abstract SecretKey getKey(Cycle.Keys keys);

    public E createEmpty(LocalDate entryDate, Cycle.Keys keys) {
      return createEmpty(entryDate, getKey(keys));
    }

    public Single<E> decryptEntry(DataSnapshot snapshot, Cycle.Keys keys) {
      final SecretKey key = getKey(keys);
      String encryptedEntry = snapshot.child(mChildId).getValue(String.class);
      return mCryptoUtil.decrypt(encryptedEntry, key, mClazz);
    }

    public Single<EncryptedEntry> encryptEntry(E entry) {
      return mCryptoUtil.encrypt(entry).map(new Function<String, EncryptedEntry>() {
        @Override
        public EncryptedEntry apply(String s) throws Exception {
          return new EncryptedEntry(mChildId, s);
        }
      });
    }
  }

  private static class ObservationEntryProvider extends SubEntryProvider<ObservationEntry> {

    public ObservationEntryProvider(CryptoUtil cryptoUtil) {
      super(ObservationEntry.class, "observation", cryptoUtil);
    }

    @Override
    ObservationEntry createEmpty(LocalDate entryDate, SecretKey key) {
      return ObservationEntry.emptyEntry(entryDate, key);
    }

    @Override
    SecretKey getKey(Cycle.Keys keys) {
      return keys.chartKey;
    }
  }

  private static class WellnessEntryProvider extends SubEntryProvider<WellnessEntry> {

    public WellnessEntryProvider(CryptoUtil cryptoUtil) {
      super(WellnessEntry.class, "wellness", cryptoUtil);
    }

    @Override
    WellnessEntry createEmpty(LocalDate entryDate, SecretKey key) {
      return WellnessEntry.emptyEntry(entryDate, key);
    }

    @Override
    SecretKey getKey(Cycle.Keys keys) {
      return keys.wellnessKey;
    }
  }

  private static class SymptomEntryProvider extends SubEntryProvider<SymptomEntry> {

    public SymptomEntryProvider(CryptoUtil cryptoUtil) {
      super(SymptomEntry.class, "symptom", cryptoUtil);
    }

    @Override
    SymptomEntry createEmpty(LocalDate entryDate, SecretKey key) {
      return SymptomEntry.emptyEntry(entryDate, key);
    }

    @Override
    SecretKey getKey(Cycle.Keys keys) {
      return keys.symptomKey;
    }
  }
}

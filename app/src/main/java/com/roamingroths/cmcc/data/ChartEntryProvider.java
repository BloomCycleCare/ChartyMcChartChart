package com.roamingroths.cmcc.data;

import android.util.Log;

import com.google.common.base.Predicate;
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
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.reactivex.functions.Function4;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by parkeroth on 11/18/17.
 */

public class ChartEntryProvider {

  private static boolean DEBUG = true;
  private static String TAG = ChartEntryProvider.class.getSimpleName();

  private final FirebaseDatabase mDB;
  private final CryptoUtil mCryptoUtil;
  private final ObservationEntryProvider mObservationEntryProvider;
  private final WellnessEntryProvider mWellnessEntryProvider;
  private final SymptomEntryProvider mSymptomEntryProvider;

  public ChartEntryProvider(FirebaseDatabase db, CryptoUtil mCryptoUtil) {
    this.mDB = db;
    this.mCryptoUtil = mCryptoUtil;
    this.mObservationEntryProvider = new ObservationEntryProvider(mCryptoUtil);
    this.mWellnessEntryProvider = new WellnessEntryProvider(mCryptoUtil);
    this.mSymptomEntryProvider = new SymptomEntryProvider(mCryptoUtil);
  }

  private ChartEntry createEmpty(LocalDate entryDate, Cycle.Keys keys) {
    ObservationEntry observation = mObservationEntryProvider.createEmpty(entryDate, keys);
    WellnessEntry wellness = mWellnessEntryProvider.createEmpty(entryDate, keys);
    SymptomEntry symptomEntry = mSymptomEntryProvider.createEmpty(entryDate, keys);

    return new ChartEntry(entryDate, observation, wellness, symptomEntry);
  }

  public Flowable<RxFirebaseChildEvent<ChartEntry>> entryStream(final Cycle cycle) {
    return RxFirebaseDatabase.observeChildEvent(reference(cycle))
        .flatMap(new Function<RxFirebaseChildEvent<DataSnapshot>, Publisher<RxFirebaseChildEvent<ChartEntry>>>() {
          @Override
          public Publisher<RxFirebaseChildEvent<ChartEntry>> apply(final RxFirebaseChildEvent<DataSnapshot> childEvent) throws Exception {
            return snapshotToEntry(cycle).apply(childEvent.getValue())
                .map(new Function<ChartEntry, RxFirebaseChildEvent<ChartEntry>>() {
                  @Override
                  public RxFirebaseChildEvent<ChartEntry> apply(ChartEntry chartEntry) throws Exception {
                    return new RxFirebaseChildEvent<ChartEntry>(childEvent.getKey(), chartEntry, childEvent.getEventType());
                  }
                }).toFlowable(BackpressureStrategy.BUFFER);
          }
        });
  }

  public Observable<ChartEntry> getEntries(final Cycle cycle) {
    return getEntries(RxFirebaseDatabase.observeSingleValueEvent(reference(cycle))
        .observeOn(Schedulers.computation())
        .flatMapObservable(new Function<DataSnapshot, ObservableSource<DataSnapshot>>() {
          @Override
          public ObservableSource<DataSnapshot> apply(final DataSnapshot dataSnapshot) throws Exception {
            return Observable.create(new ObservableOnSubscribe<DataSnapshot>() {
              @Override
              public void subscribe(ObservableEmitter<DataSnapshot> e) throws Exception {
                if (DEBUG)
                  Log.v(TAG, "Found " + dataSnapshot.getChildrenCount() + " children for " + cycle.id);
                for (DataSnapshot entrySnapshot : dataSnapshot.getChildren()) {
                  e.onNext(entrySnapshot);
                }
                e.onComplete();
              }
            });
          }
        }), cycle);
  }

  private Function<DataSnapshot, Observable<ChartEntry>> snapshotToEntry(final Cycle cycle) {
    return new Function<DataSnapshot, Observable<ChartEntry>>() {
      @Override
      public Observable<ChartEntry> apply(DataSnapshot snapshot) {
        LocalDate entryDate = DateUtil.fromWireStr(snapshot.getKey());
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

  public Observable<ChartEntry> getEntries(Observable<DataSnapshot> snapshots, final Cycle cycle) {
    return snapshots.flatMap(snapshotToEntry(cycle));
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
            return RxFirebaseDatabase.updateChildren(reference(cycle, chartEntry.entryDate), updates);
          }
        });
  }

  public Completable deleteEntry(Cycle cycle, LocalDate entryDate) {
    return RxFirebaseDatabase.removeValue(reference(cycle, entryDate));
  }

  public Completable maybeAddNewEntries(final Cycle cycle) {
    return getEntries(cycle)
        .toList()
        // find most recent entry
        .flatMapMaybe(new Function<List<ChartEntry>, MaybeSource<LocalDate>>() {
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
        })
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
            return putEntry(cycle, createEmpty(localDate, cycle.keys));
          }
        });
  }

  public Completable moveEntries(
      final Cycle fromCycle,
      final Cycle toCycle,
      final Predicate<LocalDate> datePredicate) {
    if (DEBUG) Log.v(TAG, "Move entries from " + fromCycle.id + " to " + toCycle.id);
    return getEntries(fromCycle)
        .filter(new io.reactivex.functions.Predicate<ChartEntry>() {
          @Override
          public boolean test(ChartEntry entry) throws Exception {
            return datePredicate.apply(entry.entryDate);
          }
        })
        .flatMapCompletable(new Function<ChartEntry, CompletableSource>() {
          @Override
          public CompletableSource apply(ChartEntry entry) throws Exception {
            entry.observationEntry.swapKey(mObservationEntryProvider.getKey(toCycle.keys));
            entry.wellnessEntry.swapKey(mWellnessEntryProvider.getKey(toCycle.keys));
            entry.symptomEntry.swapKey(mSymptomEntryProvider.getKey(toCycle.keys));
            return putEntry(toCycle, entry).andThen(deleteEntry(fromCycle, entry.entryDate));
          }
        });
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

    public String getChildId() {
      return mChildId;
    }

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

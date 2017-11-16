package com.roamingroths.cmcc.data;

import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.Entry;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import java.util.List;

import javax.crypto.SecretKey;

import durdinapps.rxfirebase2.RxFirebaseDatabase;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by parkeroth on 9/2/17.
 */

public abstract class EntryProvider<E extends Entry> {

  private static boolean DEBUG = false;

  private final Class<E> mClazz;
  private final String mChildId;
  private final FirebaseDatabase db;
  private final CryptoUtil mCryptoUtil;
  private final String mLogId;

  enum ChildId {
    CHART, WELLNESS, SYMPTOM
  }

  EntryProvider(FirebaseDatabase db, CryptoUtil cryptoUtil, ChildId childId, Class<E> clazz) {
    this.db = db;
    this.mCryptoUtil = Preconditions.checkNotNull(cryptoUtil);
    this.mChildId = childId.name().toLowerCase();
    this.mClazz = clazz;
    this.mLogId = "EntryProvider<" + mClazz.getName() + ">";
  }

  abstract E createEmptyEntry(LocalDate date, SecretKey key);

  public abstract SecretKey getKey(Cycle cycle);

  public Single<E> fromSnapshot(DataSnapshot snapshot, SecretKey key) {
    return mCryptoUtil.decrypt(snapshot.getValue(String.class), key, mClazz);
  }

  public final Class<E> getEntryClazz() {
    return mClazz;
  }

  public final Completable maybeAddNewEntries(final Cycle cycle) {
    return getDecryptedEntries(cycle)
        .toList()
        // find most recent entry
        .flatMapMaybe(new Function<List<E>, MaybeSource<LocalDate>>() {
          @Override
          public MaybeSource<LocalDate> apply(List<E> entries) throws Exception {
            if (entries.isEmpty()) {
              return Maybe.empty();
            }
            LocalDate lastEntryDate = null;
            for (E entry : entries) {
              LocalDate entryDate = entry.getDate();
              if (lastEntryDate == null || lastEntryDate.isBefore(entryDate)) {
                lastEntryDate = entryDate;
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
            return putEntry(cycle.id, createEmptyEntry(localDate, getKey(cycle)));
          }
        });
  }

  public Completable putEntry(final String cycleId, final E entry) {
    return Single.just(entry)
        .observeOn(Schedulers.computation())
        .flatMap(new Function<E, SingleSource<String>>() {
          @Override
          public SingleSource<String> apply(E e) throws Exception {
            return mCryptoUtil.encrypt(e);
          }
        })
        .flatMapCompletable(new Function<String, CompletableSource>() {
          @Override
          public CompletableSource apply(String encryptedStr) throws Exception {
            return RxFirebaseDatabase.setValue(reference(cycleId, entry.getDateStr()), encryptedStr);
          }
        });
  }

  public final Completable deleteEntry(String cycleId, LocalDate entryDate) {
    return RxFirebaseDatabase.removeValue(reference(cycleId, DateUtil.toWireStr(entryDate)));
  }

  public final Observable<String> getEncryptedEntries(Cycle cycle) {
    return RxFirebaseDatabase.observeSingleValueEvent(reference(cycle.id))
        .observeOn(Schedulers.computation())
        .flatMapObservable(new Function<DataSnapshot, ObservableSource<String>>() {
          @Override
          public ObservableSource<String> apply(final DataSnapshot dataSnapshot) throws Exception {
            return Observable.create(new ObservableOnSubscribe<String>() {
              @Override
              public void subscribe(ObservableEmitter<String> e) throws Exception {
                logV("Found " + dataSnapshot.getChildrenCount() + " children for " + mChildId);
                for (DataSnapshot entrySnapshot : dataSnapshot.getChildren()) {
                  e.onNext(entrySnapshot.getValue(String.class));
                }
                e.onComplete();
              }
            });
          }
        });
  }

  public final Observable<E> getDecryptedEntries(final Cycle cycle) {
    logV("Getting decrypted entries");
    return getEncryptedEntries(cycle)
        .observeOn(Schedulers.computation())
        .flatMap(new Function<String, ObservableSource<E>>() {
          @Override
          public ObservableSource<E> apply(String encryptedEntry) throws Exception {
            return mCryptoUtil.decrypt(encryptedEntry, getKey(cycle), mClazz)
                .map(new Function<E, E>() {
                  @Override
                  public E apply(E e) throws Exception {
                    e.swapKey(getKey(cycle));
                    return e;
                  }
                }).toObservable();
          }
        });
  }

  public Completable moveEntries(
      final Cycle fromCycle,
      final Cycle toCycle,
      final Predicate<LocalDate> datePredicate) {
    logV("Source cycle id: " + fromCycle.id);
    logV("Destination cycle id: " + toCycle.id);
    return getDecryptedEntries(fromCycle)
        .filter(new io.reactivex.functions.Predicate<E>() {
          @Override
          public boolean test(E entry) throws Exception {
            return datePredicate.apply(entry.getDate());
          }
        })
        .flatMapCompletable(new Function<E, CompletableSource>() {
          @Override
          public CompletableSource apply(E decryptedEntry) throws Exception {
            decryptedEntry.swapKey(getKey(toCycle));
            return putEntry(toCycle.id, decryptedEntry).andThen(deleteEntry(fromCycle.id, decryptedEntry.getDate()));
          }
        });
  }

  private DatabaseReference reference(String cycleId) {
    return db.getReference("entries").child(cycleId).child(mChildId);
  }

  private DatabaseReference reference(String cycleId, String dateStr) {
    return reference(cycleId).child(dateStr);
  }

  private void logV(String message) {
    if (DEBUG) Log.v(mLogId, message);
  }
}

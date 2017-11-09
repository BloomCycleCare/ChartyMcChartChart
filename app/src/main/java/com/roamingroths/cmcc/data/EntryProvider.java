package com.roamingroths.cmcc.data;

import android.os.AsyncTask;
import android.util.Log;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.crypto.CyrptoExceptions;
import com.roamingroths.cmcc.crypto.RxCryptoUtil;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.Entry;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.Callbacks.Callback;
import com.roamingroths.cmcc.utils.DateUtil;
import com.roamingroths.cmcc.utils.Listeners;

import org.joda.time.LocalDate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import io.reactivex.functions.Function;

/**
 * Created by parkeroth on 9/2/17.
 */

public abstract class EntryProvider<E extends Entry> {

  private static boolean DEBUG = true;

  private final Class<E> mClazz;
  private final String mChildId;
  private final FirebaseDatabase db;
  private final RxCryptoUtil mCryptoUtil;
  private final String mLogId;

  enum ChildId {
    CHART, WELLNESS, SYMPTOM
  }

  EntryProvider(FirebaseDatabase db, RxCryptoUtil cryptoUtil, ChildId childId, Class<E> clazz) {
    this.db = db;
    this.mCryptoUtil = cryptoUtil;
    this.mChildId = childId.name().toLowerCase();
    this.mClazz = clazz;
    this.mLogId = "EntryProvider<" + mClazz.getName() + ">";
  }

  abstract E createEmptyEntry(LocalDate date, SecretKey key);

  public abstract SecretKey getKey(Cycle cycle);

  @Deprecated
  abstract void fromSnapshot(DataSnapshot snapshot, SecretKey key, Callback<E> callback);

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
          public ObservableSource<LocalDate> apply(final LocalDate localDate) throws Exception {
            return Observable.create(new ObservableOnSubscribe<LocalDate>() {
              @Override
              public void subscribe(ObservableEmitter<LocalDate> e) throws Exception {
                for (LocalDate date = localDate; date.isBefore(DateUtil.now().plusDays(1)); date = date.plusDays(1)) {
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
    return mCryptoUtil.encrypt(entry)
        .flatMapCompletable(new Function<String, CompletableSource>() {
          @Override
          public CompletableSource apply(String encryptedStr) throws Exception {
            return RxFirebaseDatabase.setValue(reference(cycleId, entry.getDateStr()), encryptedStr);
          }
        });
  }

  @Deprecated
  public final void putEntry(
      final String cycleId, final E entry,
      final DatabaseReference.CompletionListener completionListener) throws CyrptoExceptions.CryptoException {
    CryptoUtil.encrypt(entry, new Callbacks.HaltingCallback<String>() {
      @Override
      public void acceptData(String encryptedEntry) {
        reference(cycleId, entry.getDateStr()).setValue(encryptedEntry, completionListener);
      }
    });
  }

  public final Completable deleteEntry(String cycleId, LocalDate entryDate) {
    return RxFirebaseDatabase.removeValue(reference(cycleId, DateUtil.toWireStr(entryDate)));
  }

  public Maybe<E> getEntry(Cycle cycle, String entryDateStr) {
    final SecretKey key = getKey(cycle);
    return RxFirebaseDatabase.observeSingleValueEvent(reference(cycle.id, entryDateStr))
        .flatMap(new Function<DataSnapshot, MaybeSource<E>>() {
          @Override
          public MaybeSource<E> apply(DataSnapshot snapshot) throws Exception {
            return fromSnapshot(snapshot, key).toMaybe();
          }
        });
  }

  @Deprecated
  public final void getEncryptedEntries(
      String cycleId, final Callback<Map<LocalDate, String>> callback) {
    reference(cycleId).addListenerForSingleValueEvent(new Listeners.SimpleValueEventListener(callback) {
      @Override
      public void onDataChange(final DataSnapshot entrySnapshots) {
        new AsyncTask<Void, Integer, Void>() {
          @Override
          protected Void doInBackground(Void... params) {
            Map<LocalDate, String> entries = new HashMap<>();
            for (DataSnapshot entrySnapshot : entrySnapshots.getChildren()) {
              LocalDate entryDate = DateUtil.fromWireStr(entrySnapshot.getKey());
              entries.put(entryDate, entrySnapshot.getValue(String.class));
            }
            callback.acceptData(entries);
            return null;
          }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      }
    });
  }

  public final Observable<String> getEncryptedEntries(Cycle cycle) {
    return RxFirebaseDatabase.observeSingleValueEvent(reference(cycle.id))
        .flatMapObservable(new Function<DataSnapshot, ObservableSource<String>>() {
          @Override
          public ObservableSource<String> apply(final DataSnapshot dataSnapshot) throws Exception {
            return Observable.create(new ObservableOnSubscribe<String>() {
              @Override
              public void subscribe(ObservableEmitter<String> e) throws Exception {
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
    return getEncryptedEntries(cycle)
        .flatMap(new Function<String, ObservableSource<E>>() {
          @Override
          public ObservableSource<E> apply(String encryptedEntry) throws Exception {
            return mCryptoUtil.decrypt(encryptedEntry, getKey(cycle), mClazz).toObservable();
          }
        });
  }

  @Deprecated
  public final void getDecryptedEntries(final Cycle cycle, final Callback<Map<LocalDate, E>> callback) {
    getEncryptedEntries(cycle.id, new Callbacks.ErrorForwardingCallback<Map<LocalDate, String>>(callback) {
      @Override
      public void acceptData(final Map<LocalDate, String> encryptedEntries) {
        final Map<LocalDate, E> decryptedEntries = Maps.newConcurrentMap();
        final Set<LocalDate> brokenEntries = Sets.newConcurrentHashSet();
        logV("Found " + encryptedEntries.size() + " entries to decrypt.");
        if (encryptedEntries.isEmpty()) {
          callback.acceptData(ImmutableMap.<LocalDate, E>of());
        }
        for (Map.Entry<LocalDate, String> entry : encryptedEntries.entrySet()) {
          final LocalDate entryDate = entry.getKey();
          final String encryptedEntry = entry.getValue();
          CryptoUtil.decrypt(encryptedEntry, getKey(cycle), mClazz, new Callbacks.Callback<E>() {
            @Override
            public void acceptData(E decryptedEntry) {
              decryptedEntries.put(decryptedEntry.getDate(), decryptedEntry);
              maybeRespond();
            }

            @Override
            public void handleNotFound() {
              logV("Decrypted entry not found for " + entryDate);
              brokenEntries.add(entryDate);
              maybeRespond();
            }

            @Override
            public void handleError(DatabaseError error) {
              logV("Error decrypting entry for " + entryDate);
              brokenEntries.add(entryDate);
              maybeRespond();
            }

            private synchronized void maybeRespond() {
              if (decryptedEntries.size() + brokenEntries.size() == encryptedEntries.size()) {
                if (brokenEntries.size() == 0) {
                  callback.acceptData(decryptedEntries);
                } else {
                  callback.handleError(DatabaseError.fromException(new IllegalStateException()));
                }
              }
            }
          });
        }
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

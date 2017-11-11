package com.roamingroths.cmcc.data;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.CryptoUtil;
import com.roamingroths.cmcc.crypto.RxCryptoUtil;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.Entry;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import durdinapps.rxfirebase2.RxFirebaseDatabase;
import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.Functions;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by parkeroth on 9/2/17.
 */
public class CycleProvider {

  // TODO: Remove FirebaseAuth stuff

  private static final boolean DEBUG = true;
  private static final String TAG = CycleProvider.class.getSimpleName();

  private final FirebaseDatabase db;
  private final RxCryptoUtil cryptoUtil;
  private final CycleKeyProvider cycleKeyProvider;
  private final ImmutableMap<Class<? extends Entry>, EntryProvider> entryProviders;

  public static CycleProvider forDb(FirebaseDatabase db) {
    RxCryptoUtil cryptoUtil = CryptoProvider.forDb(db).createCryptoUtil().blockingGet();
    return forDb(db, cryptoUtil);
  }

  public static CycleProvider forDb(FirebaseDatabase db, RxCryptoUtil cryptoUtil) {
    return new CycleProvider(
        db,
        cryptoUtil,
        CycleKeyProvider.forDb(db, cryptoUtil),
        ChartEntryProvider.forDb(db, cryptoUtil),
        WellnessEntryProvider.forDb(db, cryptoUtil),
        SymptomEntryProvider.forDb(db, cryptoUtil));
  }

  private CycleProvider(
      FirebaseDatabase db, RxCryptoUtil cryptoUtil, CycleKeyProvider cycleKeyProvider, ChartEntryProvider chartEntryProvider, WellnessEntryProvider wellnessEntryProvider, SymptomEntryProvider symptomEntryProvider) {
    this.db = db;
    this.cryptoUtil = cryptoUtil;
    this.cycleKeyProvider = cycleKeyProvider;
    entryProviders = ImmutableMap.<Class<? extends Entry>, EntryProvider>builder()
        .put(chartEntryProvider.getEntryClazz(), chartEntryProvider)
        .put(wellnessEntryProvider.getEntryClazz(), wellnessEntryProvider)
        .put(symptomEntryProvider.getEntryClazz(), symptomEntryProvider).build();
  }

  public <E extends Entry> EntryProvider<E> getProviderForEntry(E entry) {
    return getProviderForClazz((Class<E>) entry.getClass());
  }

  public <E extends Entry> EntryProvider<E> getProviderForClazz(Class<E> clazz) {
    return entryProviders.get(clazz);
  }

  public Collection<EntryProvider> getEntryProviders() {
    return entryProviders.values();
  }

  public CycleKeyProvider getCycleKeyProvider() {
    return cycleKeyProvider;
  }

  public void attachListener(ChildEventListener listener, String userId) {
    DatabaseReference ref = reference(userId);
    ref.addChildEventListener(listener);
    ref.keepSynced(true);
  }

  public Completable maybeCreateNewEntries(Cycle cycle) {
    if (cycle.endDate != null) {
      logV("No entries to add, end date set");
      return Completable.complete();
    }
    List<Completable> results = new ArrayList<>(entryProviders.size());
    for (EntryProvider provider : entryProviders.values()) {
      results.add(provider.maybeAddNewEntries(cycle));
    }
    return Completable.merge(results);
  }

  public void detachListener(ChildEventListener listener, String userId) {
    reference(userId).removeEventListener(listener);
  }

  public Completable putCycleRx(final String userId, final Cycle cycle) {
    Map<String, Object> updates = new HashMap<>();
    updates.put("previous-cycle-id", cycle.previousCycleId);
    updates.put("next-cycle-id", cycle.nextCycleId);
    updates.put("start-date", cycle.startDateStr);
    updates.put("end-date", DateUtil.toWireStr(cycle.endDate));
    return RxFirebaseDatabase.updateChildren(reference(userId, cycle.id), updates)
        .andThen(cycleKeyProvider.putChartKeysRx(cycle.keys, cycle.id, userId));
  }

  public Single<Cycle> createCycleRx(
      final String userId,
      @Nullable Cycle previousCycle,
      @Nullable Cycle nextCycle,
      final LocalDate startDate,
      final @Nullable LocalDate endDate) {
    DatabaseReference cycleRef = reference(userId).push();
    logV("Creating new cycle: " + cycleRef.getKey());
    final String cycleId = cycleRef.getKey();
    Cycle.Keys keys = new Cycle.Keys(
        CryptoUtil.createSecretKey(), CryptoUtil.createSecretKey(), CryptoUtil.createSecretKey());
    final Cycle cycle = new Cycle(
        cycleId,
        (previousCycle == null) ? null : previousCycle.id,
        (nextCycle == null) ? null : nextCycle.id,
        startDate,
        endDate,
        keys);
    return putCycleRx(userId, cycle).andThen(Single.just(cycle));
  }

  public Maybe<Cycle> getCurrentCycle(final String userId) {
    return getAllCycles(userId)
        .filter(new io.reactivex.functions.Predicate<Cycle>() {
          @Override
          public boolean test(@NonNull Cycle cycle) throws Exception {
            return cycle.endDate == null;
          }
        })
        .firstElement();
  }

  public Single<Cycle> getOrCreateCurrentCycle(final String userId, Single<LocalDate> startOfFirstCycle) {
    return getCurrentCycle(userId)
        .switchIfEmpty(startOfFirstCycle.flatMapMaybe(new Function<LocalDate, MaybeSource<? extends Cycle>>() {
          @Override
          public MaybeSource<? extends Cycle> apply(@NonNull LocalDate startDate) throws Exception {
            return createCycleRx(userId, null, null, startDate, null).toMaybe();
          }
        }))
        .toSingle();
  }

  public Observable<Cycle> getAllCycles(final String userId) {
    logV("Fetching cycles");
    return RxFirebaseDatabase.observeSingleValueEvent(reference(userId), Functions.<DataSnapshot>identity())
        .flatMapObservable(new Function<DataSnapshot, ObservableSource<DataSnapshot>>() {
          @Override
          public ObservableSource<DataSnapshot> apply(DataSnapshot snapshot) throws Exception {

            return Observable.fromIterable(snapshot.getChildren());
          }
        })
        .flatMap(new Function<DataSnapshot, ObservableSource<Cycle>>() {
          @Override
          public ObservableSource<Cycle> apply(DataSnapshot snapshot) throws Exception {
            return cycleKeyProvider.getChartKeys(snapshot.getKey(), userId).map(Cycle.fromSnapshot(snapshot)).toObservable();
          }
        });
  }

  private Completable dropCycle(String cycleId, String userId) {
    Completable dropEntries = RxFirebaseDatabase.removeValue(db.getReference("entries").child(cycleId));
    Completable dropKeys = cycleKeyProvider.dropKeys(cycleId);
    Completable dropCycle = RxFirebaseDatabase.removeValue(reference(userId, cycleId));
    return Completable.mergeArray(dropEntries, dropKeys, dropCycle);
  }

  public Completable dropCycles() {
    final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    DatabaseReference referenceToCycles = reference(user.getUid());
    return RxFirebaseDatabase.observeSingleValueEvent(referenceToCycles)
        .flatMapCompletable(new Function<DataSnapshot, CompletableSource>() {
          @Override
          public CompletableSource apply(@NonNull DataSnapshot snapshot) throws Exception {
            List<Completable> completables = new ArrayList<>();
            for (DataSnapshot cycleSnapshot : snapshot.getChildren()) {
              completables.add(dropCycle(cycleSnapshot.getKey(), user.getUid()));
            }
            return Completable.merge(completables);
          }
        });
  }

  public Maybe<Cycle> getCycle(final String userId, final @Nullable String cycleId) {
    if (Strings.isNullOrEmpty(cycleId)) {
      return Maybe.empty();
    }
    return cycleKeyProvider.getChartKeys(cycleId, userId)
        .flatMap(new Function<Cycle.Keys, MaybeSource<Cycle>>() {
          @Override
          public MaybeSource<Cycle> apply(final Cycle.Keys keys) throws Exception {
            DatabaseReference referenceToCycle = reference(userId, cycleId);
            return RxFirebaseDatabase.observeSingleValueEvent(referenceToCycle)
                .map(new Function<DataSnapshot, Cycle>() {
                  @Override
                  public Cycle apply(DataSnapshot dataSnapshot) throws Exception {
                    return Cycle.fromSnapshot(dataSnapshot, keys);
                  }
                });
          }
        });
  }

  public Single<Cycle> combineCycleRx(final String userId, final Cycle currentCycle) {
    Single<Cycle> previousCycle = getCycle(userId, currentCycle.id).toSingle().cache();

    Single<Cycle> updateNextAndReturnPrevious = previousCycle
        .flatMap(new Function<Cycle, Single<Cycle>>() {
          @Override
          public Single<Cycle> apply(Cycle previousCycle) throws Exception {
            if (previousCycle.nextCycleId == null) {
              if (DEBUG) Log.v(TAG, "No next cycle, skipping update");
              return Single.just(previousCycle);
            }
            if (DEBUG) Log.v(TAG, currentCycle.nextCycleId + " previous -> " + previousCycle.id);
            previousCycle.nextCycleId = currentCycle.nextCycleId;
            return RxFirebaseDatabase.setValue(reference(userId, currentCycle.nextCycleId).child("previous_cycle_id"), previousCycle.id)
                .andThen(Single.just(previousCycle));
          }
        });

    Completable updatePrevious = previousCycle
        .flatMapCompletable(new Function<Cycle, CompletableSource>() {
          @Override
          public CompletableSource apply(Cycle previousCycle) throws Exception {
            if (DEBUG) Log.v(TAG, "Updating previous cycle fields");
            Map<String, Object> updates = new HashMap<>();
            updates.put("next-cycle-id", currentCycle.nextCycleId);
            previousCycle.nextCycleId = currentCycle.nextCycleId;
            updates.put("end-date", DateUtil.toWireStr(currentCycle.endDate));
            previousCycle.endDate = currentCycle.endDate;
            return RxFirebaseDatabase.updateChildren(reference(userId, previousCycle.id), updates);
          }
        });

    Completable moveEntries = previousCycle
        .flatMapCompletable(new Function<Cycle, CompletableSource>() {
          @Override
          public CompletableSource apply(Cycle previousCycle) throws Exception {
            if (DEBUG) Log.v(TAG, "Moving entries");
            Set<Completable> entryMoves = new HashSet<>();
            for (final EntryProvider provider : entryProviders.values()) {
              entryMoves.add(provider.moveEntries(currentCycle, previousCycle, Predicates.alwaysTrue()));
            }
            return Completable.merge(entryMoves);
          }
        }).andThen(cycleKeyProvider.dropKeys(currentCycle.id));

    Completable dropCycle = dropCycle(currentCycle.id, userId);

    return Completable.mergeArray(updatePrevious, moveEntries).andThen(dropCycle).andThen(updateNextAndReturnPrevious);
  }

  public Single<Cycle> splitCycleRx(final String userId, final Cycle currentCycle, Single<ChartEntry> firstEntry) {
    return firstEntry.flatMap(new Function<ChartEntry, SingleSource<Cycle>>() {
      @Override
      public SingleSource<Cycle> apply(final ChartEntry firstEntry) throws Exception {
        if (DEBUG) Log.v(TAG, "First entry: " + firstEntry.getDateStr());

        Single<Cycle> newCycle = getCycle(userId, currentCycle.nextCycleId)
            .flatMap(new Function<Cycle, MaybeSource<Cycle>>() {
              @Override
              public MaybeSource<Cycle> apply(Cycle nextCycle) throws Exception {
                if (DEBUG) Log.v(TAG, "Create new cycle with next");
                return createCycleRx(
                    userId, currentCycle, nextCycle, firstEntry.getDate(), currentCycle.endDate)
                    .toMaybe();
              }
            })
            .switchIfEmpty(
                createCycleRx(userId, currentCycle, null, firstEntry.getDate(), currentCycle.endDate).toMaybe())
            .toSingle().cache();

        Completable updateNextPrevious = newCycle.flatMapCompletable(new Function<Cycle, Completable>() {
          @Override
          public Completable apply(Cycle newCycle) throws Exception {
            if (DEBUG) Log.v(TAG, "Update next's previous.");
            if (Strings.isNullOrEmpty(newCycle.nextCycleId)) {
              return Completable.complete().doOnComplete(new Action() {
                @Override
                public void run() throws Exception {
                  if (DEBUG) Log.v(TAG, "Done updating next's previous.");
                }
              });
            }
            return RxFirebaseDatabase.setValue(reference(userId, newCycle.nextCycleId).child("previous-cycle-id"), newCycle.id).doOnComplete(new Action() {
              @Override
              public void run() throws Exception {
                if (DEBUG) Log.v(TAG, "Done updating next's previous.");
              }
            });
          }
        });

        Completable updateCurrentNext = newCycle.flatMapCompletable(new Function<Cycle, Completable>() {
          @Override
          public Completable apply(Cycle newCycle) throws Exception {
            if (DEBUG) Log.v(TAG, "Update current's fields.");
            Map<String, Object> updates = new HashMap<>();
            updates.put("next-cycle-id", newCycle.id);
            updates.put("end-date", DateUtil.toWireStr(firstEntry.getDate().minusDays(1)));
            return RxFirebaseDatabase.updateChildren(reference(userId, currentCycle.id), updates).doOnComplete(new Action() {
              @Override
              public void run() throws Exception {
                if (DEBUG) Log.v(TAG, "Done updating current's fields.");
              }
            }).subscribeOn(Schedulers.io());
          }
        });

        Completable moveEntries = newCycle.flatMapCompletable(new Function<Cycle, CompletableSource>() {
          @Override
          public CompletableSource apply(Cycle newCycle) throws Exception {
            if (DEBUG) Log.v(TAG, "Moving entries.");
            final Predicate<LocalDate> ifEqualOrAfter = new Predicate<LocalDate>() {
              @Override
              public boolean apply(LocalDate entryDate) {
                return entryDate.equals(firstEntry.getDate()) || entryDate.isAfter(firstEntry.getDate());
              }
            };
            Set<Completable> entryMoves = new HashSet<>();
            for (final EntryProvider provider : entryProviders.values()) {
              entryMoves.add(provider.moveEntries(currentCycle, newCycle, ifEqualOrAfter));
            }
            return Completable.merge(entryMoves).doOnComplete(new Action() {
              @Override
              public void run() throws Exception {
                if (DEBUG) Log.v(TAG, "Done moving entries.");
              }
            });
          }
        });

        return Completable.mergeArray(updateNextPrevious, updateCurrentNext, moveEntries)
            .andThen(newCycle)
            .doOnSuccess(new Consumer<Cycle>() {
              @Override
              public void accept(Cycle cycle) throws Exception {
                if (DEBUG) Log.v(TAG, "Returning cycle: " + cycle.id);
              }
            });
      }
    });
  }

  private DatabaseReference reference(String userId, String cycleId) {
    return reference(userId).child(cycleId);
  }

  @Deprecated
  private DatabaseReference reference(String userId) {
    return db.getReference("cycles").child(userId);
  }

  private DatabaseReference reference(FirebaseUser user) {
    return db.getReference("cycles").child(user.getUid());
  }

  private void logV(String message) {
    Log.v("CycleProvider", message);
  }
}

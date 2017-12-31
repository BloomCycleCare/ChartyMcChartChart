package com.roamingroths.cmcc.data;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.crypto.AesCryptoUtil;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;
import com.roamingroths.cmcc.logic.ObservationEntry;
import com.roamingroths.cmcc.ui.entry.detail.EntrySaveResult;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Function3;
import io.reactivex.internal.functions.Functions;

/**
 * Created by parkeroth on 9/2/17.
 */
public class CycleProvider {

  // TODO: Remove FirebaseAuth stuff

  private static final boolean DEBUG = true;
  private static final String TAG = CycleProvider.class.getSimpleName();

  private final FirebaseDatabase db;
  private final CycleKeyProvider cycleKeyProvider;
  private final ChartEntryProvider chartEntryProvider;
  private final Map<String, Cycle> mCycleCache;

  public CycleProvider(
      FirebaseDatabase db, CycleKeyProvider cycleKeyProvider, ChartEntryProvider chartEntryProvider) {
    this.db = db;
    this.cycleKeyProvider = cycleKeyProvider;
    this.chartEntryProvider = chartEntryProvider;
    this.mCycleCache = Maps.newConcurrentMap();
  }

  public Completable initCache(final FirebaseUser user) {
    return getAllFromRemote(user).flatMapCompletable(new Function<Cycle, CompletableSource>() {
      @Override
      public CompletableSource apply(Cycle cycle) throws Exception {
        if (DEBUG) Log.v(TAG, "Caching cycle " + cycle.id);
        mCycleCache.put(cycle.id, cycle);
        return Completable.complete();
      }
    });
  }

  public ChartEntryProvider getEntryProvider() {
    return chartEntryProvider;
  }

  public Observable<ChartEntry> getEntries(Cycle cycle) {
    return chartEntryProvider.getEntries(cycle);
  }

  @Deprecated
  public Completable maybeCreateNewEntries(Cycle cycle) {
    return chartEntryProvider.maybeAddNewEntries(cycle);
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

  private Single<Cycle> createCycle(
      final String userId,
      @Nullable Cycle previousCycle,
      @Nullable Cycle nextCycle,
      final LocalDate startDate,
      final @Nullable LocalDate endDate) {
    DatabaseReference cycleRef = reference(userId).push();
    logV("Creating new cycle: " + cycleRef.getKey());
    final String cycleId = cycleRef.getKey();
    try {
      Cycle.Keys keys = new Cycle.Keys(
          AesCryptoUtil.createKey(), AesCryptoUtil.createKey(), AesCryptoUtil.createKey());
      final Cycle cycle = new Cycle(
          cycleId,
          (previousCycle == null) ? null : previousCycle.id,
          (nextCycle == null) ? null : nextCycle.id,
          startDate,
          endDate,
          keys);
      return putCycleRx(userId, cycle).andThen(Single.just(cycle));
    } catch (Exception e) {
      return Single.error(e);
    }
  }

  public Maybe<Cycle> getCurrentCycle(final FirebaseUser user) {
    return getAllCycles(user)
        .filter(new io.reactivex.functions.Predicate<Cycle>() {
          @Override
          public boolean test(@NonNull Cycle cycle) throws Exception {
            return cycle.endDate == null;
          }
        })
        .firstElement();
  }

  public Single<Cycle> getOrCreateCurrentCycle(final FirebaseUser user, Single<LocalDate> startOfFirstCycle) {
    return getCurrentCycle(user)
        .switchIfEmpty(startOfFirstCycle.flatMapMaybe(new Function<LocalDate, MaybeSource<? extends Cycle>>() {
          @Override
          public MaybeSource<? extends Cycle> apply(@NonNull LocalDate startDate) throws Exception {
            return createCycle(user.getUid(), null, null, startDate, null)
                .doOnSuccess(new Consumer<Cycle>() {
                  @Override
                  public void accept(Cycle cycle) throws Exception {
                    mCycleCache.put(cycle.id, cycle);
                  }
                }).toMaybe();
          }
        }))
        .toSingle();
  }

  public Observable<Cycle> getCachedCycles() {
    return Observable.fromIterable(mCycleCache.values());
  }

  public Observable<Cycle> getAllCycles(final FirebaseUser user) {
    return Observable.fromIterable(mCycleCache.values());
  }

  private Observable<Cycle> getAllFromRemote(final FirebaseUser user) {
    logV("Fetching cycles");
    return RxFirebaseDatabase.observeSingleValueEvent(reference(user), Functions.<DataSnapshot>identity())
        .flatMapObservable(new Function<DataSnapshot, ObservableSource<DataSnapshot>>() {
          @Override
          public ObservableSource<DataSnapshot> apply(DataSnapshot snapshot) throws Exception {
            return Observable.fromIterable(snapshot.getChildren());
          }
        })
        .flatMap(new Function<DataSnapshot, ObservableSource<Cycle>>() {
          @Override
          public ObservableSource<Cycle> apply(DataSnapshot snapshot) throws Exception {
            return cycleKeyProvider.getChartKeys(snapshot.getKey(), user.getUid()).map(Cycle.fromSnapshot(snapshot)).toObservable();
          }
        });
  }

  private Completable dropCycle(String cycleId, String userId) {
    if (DEBUG) Log.v(TAG, "Dropping cycle: " + cycleId);
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

  private Maybe<Cycle> getCycle(final String userId, final @Nullable String cycleId) {
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

  public Single<EntrySaveResult> combineCycleRx(final String userId, final Cycle currentCycle) {
    Single<Cycle> previousCycle = getCycle(userId, currentCycle.previousCycleId).toSingle().cache();
    Maybe<Cycle> nextCycle = currentCycle.previousCycleId == null
        ? Maybe.<Cycle>empty() : getCycle(userId, currentCycle.previousCycleId).cache();

    Completable moveEntries = previousCycle
        .flatMapCompletable(new Function<Cycle, CompletableSource>() {
          @Override
          public CompletableSource apply(Cycle previousCycle) throws Exception {
            if (DEBUG) Log.v(TAG, "Moving entries");
            return chartEntryProvider.moveEntries(currentCycle, previousCycle, Predicates.<LocalDate>alwaysTrue());
          }
        }).andThen(cycleKeyProvider.dropKeys(currentCycle.id));

    Maybe<Cycle> updateNext = nextCycle
        .flatMap(new Function<Cycle, MaybeSource<Cycle>>() {
          @Override
          public MaybeSource<Cycle> apply(Cycle nextCycle) throws Exception {
            nextCycle.previousCycleId = currentCycle.previousCycleId;
            return RxFirebaseDatabase.setValue(
                reference(userId, nextCycle.id).child("previous_cycle_id"),
                currentCycle.previousCycleId).andThen(Maybe.just(nextCycle));
          }
        });

    Single<Cycle> updatePrevious = previousCycle
        .flatMap(new Function<Cycle, Single<Cycle>>() {
          @Override
          public Single<Cycle> apply(Cycle previousCycle) throws Exception {
            if (DEBUG) Log.v(TAG, "Updating previous cycle fields");
            Map<String, Object> updates = new HashMap<>();
            updates.put("next-cycle-id", currentCycle.nextCycleId);
            previousCycle.nextCycleId = currentCycle.nextCycleId;
            updates.put("end-date", DateUtil.toWireStr(currentCycle.endDate));
            previousCycle.endDate = currentCycle.endDate;
            return RxFirebaseDatabase.updateChildren(reference(userId, previousCycle.id), updates)
                .andThen(Single.just(previousCycle));
          }
        }).cache();

    Maybe<EntrySaveResult> result =
        Maybe.zip(updatePrevious.toMaybe(), updateNext, new BiFunction<Cycle, Cycle, EntrySaveResult>() {
          @Override
          public EntrySaveResult apply(Cycle previousCycle, Cycle nextCycle) throws Exception {
            EntrySaveResult result = new EntrySaveResult(previousCycle);
            result.droppedCycles.add(currentCycle);
            result.changedCycles.add(previousCycle);
            result.changedCycles.add(nextCycle);
            return result;
          }
        })
        .switchIfEmpty(previousCycle.flatMapMaybe(new Function<Cycle, MaybeSource<EntrySaveResult>>() {
          @Override
          public MaybeSource<EntrySaveResult> apply(Cycle previousCycle) throws Exception {
            EntrySaveResult result = new EntrySaveResult(previousCycle);
            result.droppedCycles.add(currentCycle);
            result.changedCycles.add(previousCycle);
            return Maybe.just(result);
          }
        }));

    return moveEntries
        .andThen(updatePrevious).toCompletable()
        .andThen(dropCycle(currentCycle.id, userId))
        .andThen(result.toSingle());
  }

  public Single<EntrySaveResult> splitCycleRx(final String userId, final Cycle currentCycle, Single<ObservationEntry> firstEntry) {
    return firstEntry.flatMap(new Function<ObservationEntry, SingleSource<EntrySaveResult>>() {
      @Override
      public SingleSource<EntrySaveResult> apply(final ObservationEntry firstEntry) throws Exception {
        if (DEBUG) Log.v(TAG, "First entry: " + firstEntry.getDateStr());

        final Single<Cycle> newCycle = getCycle(userId, currentCycle.nextCycleId)
            // Create new cycle assuming the next cycle as found
            .flatMap(new Function<Cycle, MaybeSource<Cycle>>() {
              @Override
              public MaybeSource<Cycle> apply(Cycle nextCycle) throws Exception {
                if (DEBUG) Log.v(TAG, "Create new cycle with next");
                return createCycle(
                    userId, currentCycle, nextCycle, firstEntry.getDate(), currentCycle.endDate).toMaybe();
              }
            })
            // If not create a new cycle with no next
            .switchIfEmpty(
                createCycle(userId, currentCycle, null, firstEntry.getDate(), currentCycle.endDate).toMaybe())
            .toSingle().cache();

        Maybe<Cycle> nextCycle = currentCycle.previousCycleId == null
            ? Maybe.<Cycle>empty() : getCycle(userId, currentCycle.previousCycleId).cache();

        Maybe<Cycle> updatedNextCycle = nextCycle.flatMap(new Function<Cycle, MaybeSource<Cycle>>() {
          @Override
          public MaybeSource<Cycle> apply(final Cycle nextCycle) throws Exception {
            return newCycle.flatMapMaybe(new Function<Cycle, MaybeSource<Cycle>>() {
              @Override
              public MaybeSource<Cycle> apply(final Cycle newCycle) throws Exception {
                nextCycle.previousCycleId = newCycle.id;
                return RxFirebaseDatabase.setValue(reference(userId, nextCycle.id).child("previous-cycle-id"), newCycle.id)
                    .andThen(Maybe.just(nextCycle));
              }
            });
          }
        });

        Single<Cycle> updatedCurrentCycle = newCycle.flatMap(new Function<Cycle, Single<Cycle>>() {
          @Override
          public Single<Cycle> apply(Cycle newCycle) throws Exception {
            if (DEBUG) Log.v(TAG, "Update current's fields.");
            Map<String, Object> updates = new HashMap<>();
            updates.put("next-cycle-id", newCycle.id);
            currentCycle.nextCycleId = newCycle.id;
            LocalDate endDate = firstEntry.getDate().minusDays(1);
            updates.put("end-date", DateUtil.toWireStr(endDate));
            currentCycle.endDate = endDate;
            return RxFirebaseDatabase.updateChildren(reference(userId, currentCycle.id), updates)
                .andThen(Single.just(currentCycle));
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
            return chartEntryProvider.moveEntries(currentCycle, newCycle, ifEqualOrAfter);
          }
        });

        return moveEntries.andThen(Maybe.zip(newCycle.toMaybe(), updatedNextCycle, updatedCurrentCycle.toMaybe(), new Function3<Cycle, Cycle, Cycle, EntrySaveResult>() {
          @Override
          public EntrySaveResult apply(Cycle newCycle, Cycle nextCycle, Cycle currentCycle) throws Exception {
            EntrySaveResult result = new EntrySaveResult(newCycle);
            result.changedCycles.add(currentCycle);
            result.changedCycles.add(nextCycle);
            result.newCycles.add(newCycle);
            return result;
          }
        }).switchIfEmpty(Single.zip(newCycle, updatedCurrentCycle, new BiFunction<Cycle, Cycle, EntrySaveResult>() {
          @Override
          public EntrySaveResult apply(Cycle newCycle, Cycle currentCycle) throws Exception {
            EntrySaveResult result = new EntrySaveResult(newCycle);
            result.changedCycles.add(currentCycle);
            result.newCycles.add(newCycle);
            return result;
          }
        }).toMaybe())).toSingle();
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

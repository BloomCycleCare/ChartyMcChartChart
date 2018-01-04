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
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.logic.Cycle;
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
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
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
        if (DEBUG) Log.v(TAG, "Caching cycleToShow " + cycle.id);
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

  @Deprecated
  public Completable putCycleRx(final String userId, final Cycle cycle) {
    Map<String, Object> updates = new HashMap<>();
    updates.put("previous-cycleToShow-id", cycle.previousCycleId);
    updates.put("next-cycleToShow-id", cycle.nextCycleId);
    updates.put("start-date", cycle.startDateStr);
    updates.put("end-date", DateUtil.toWireStr(cycle.endDate));
    return RxFirebaseDatabase.updateChildren(reference(userId, cycle.id), updates)
        .andThen(cycleKeyProvider.putChartKeysRx(cycle.keys, cycle.id, userId))
        .doOnComplete(new Action() {
          @Override
          public void run() throws Exception {
            mCycleCache.put(cycle.id, cycle);
          }
        });
  }

  private String getNewId(FirebaseUser user) {
    return reference(user).push().getKey();
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
            Cycle cycle = Cycle.builder(getNewId(user), startDate).build();
            return putCycleRx(user.getUid(), cycle).andThen(Maybe.just(cycle)).doOnSuccess(new Consumer<Cycle>() {
              @Override
              public void accept(Cycle cycle) throws Exception {
                mCycleCache.put(cycle.id, cycle);
              }
            });
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

  private Completable dropCycle(final String cycleId, String userId) {
    if (DEBUG) Log.v(TAG, "Dropping cycleToShow: " + cycleId);
    Completable dropEntries = RxFirebaseDatabase.removeValue(db.getReference("entries").child(cycleId));
    Completable dropKeys = cycleKeyProvider.dropKeys(cycleId);
    Completable dropCycle = RxFirebaseDatabase.removeValue(reference(userId, cycleId));
    return Completable.mergeArray(dropEntries, dropKeys, dropCycle).doOnComplete(new Action() {
      @Override
      public void run() throws Exception {
        mCycleCache.remove(cycleId);
      }
    });
  }

  public Completable dropCycles() {
    final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    DatabaseReference referenceToCycles = reference(user);
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

  public Maybe<EntrySaveResult> combineCycleRx(
      final FirebaseUser user,
      final Cycle currentCycle,
      final Consumer<String> updateConsumer) {
    Single<Cycle> previousCycle = getCycle(user.getUid(), currentCycle.previousCycleId).toSingle().cache();
    Maybe<Cycle> nextCycle = currentCycle.nextCycleId == null
        ? Maybe.<Cycle>empty() : getCycle(user.getUid(), currentCycle.nextCycleId).cache();

    Completable moveEntries = previousCycle
        .flatMapCompletable(new Function<Cycle, CompletableSource>() {
          @Override
          public CompletableSource apply(Cycle previousCycle) throws Exception {
            if (DEBUG) Log.v(TAG, "Moving entries");
            return chartEntryProvider.moveEntries(currentCycle, previousCycle, Predicates.<LocalDate>alwaysTrue());
          }
        }).andThen(cycleKeyProvider.dropKeys(currentCycle.id));
    moveEntries.doOnSubscribe(new Consumer<Disposable>() {
      @Override
      public void accept(Disposable disposable) throws Exception {
        updateConsumer.accept("Moving entries");
      }
    });

    Maybe<Cycle> updateNext = nextCycle
        .flatMap(new Function<Cycle, MaybeSource<Cycle>>() {
          @Override
          public MaybeSource<Cycle> apply(Cycle nextCycle) throws Exception {
            nextCycle.previousCycleId = currentCycle.previousCycleId;
            return RxFirebaseDatabase.setValue(
                reference(user.getUid(), nextCycle.id).child("previous-cycleToShow-id"),
                currentCycle.previousCycleId).andThen(Maybe.just(nextCycle));
          }
        });

    Single<Cycle> updatePrevious = previousCycle
        .flatMap(new Function<Cycle, Single<Cycle>>() {
          @Override
          public Single<Cycle> apply(Cycle previousCycle) throws Exception {
            if (DEBUG) Log.v(TAG, "Updating previous cycleToShow fields");
            Map<String, Object> updates = new HashMap<>();
            updates.put("next-cycleToShow-id", currentCycle.nextCycleId);
            previousCycle.nextCycleId = currentCycle.nextCycleId;
            updates.put("end-date", DateUtil.toWireStr(currentCycle.endDate));
            previousCycle.endDate = currentCycle.endDate;
            return RxFirebaseDatabase.updateChildren(reference(user.getUid(), previousCycle.id), updates)
                .andThen(Single.just(previousCycle));
          }
        }).cache();

    Maybe<EntrySaveResult> result =
        Maybe.zip(updatePrevious.toMaybe(), updateNext, new BiFunction<Cycle, Cycle, EntrySaveResult>() {
          @Override
          public EntrySaveResult apply(Cycle previousCycle, Cycle nextCycle) throws Exception {
            EntrySaveResult result = EntrySaveResult.forCycle(previousCycle);
            result.droppedCycles.add(currentCycle);
            result.changedCycles.add(previousCycle);
            result.changedCycles.add(nextCycle);
            return result;
          }
        })
        .switchIfEmpty(previousCycle.flatMapMaybe(new Function<Cycle, MaybeSource<EntrySaveResult>>() {
          @Override
          public MaybeSource<EntrySaveResult> apply(Cycle previousCycle) throws Exception {
            EntrySaveResult result = EntrySaveResult.forCycle(previousCycle);
            result.droppedCycles.add(currentCycle);
            result.changedCycles.add(previousCycle);
            return Maybe.just(result);
          }
        }));

    return moveEntries
        .andThen(updatePrevious).toCompletable()
        .andThen(dropCycle(currentCycle.id, user.getUid()))
        .andThen(result.toSingle())
        .toMaybe();
  }

  public Single<EntrySaveResult> splitCycleRx(
      final FirebaseUser user,
      final Cycle currentCycle,
      Single<LocalDate> firstEntryDate,
      final Consumer<String> updateConsumer) {
    Single<LocalDate> cachedFirstEntryDate = firstEntryDate.cache();
    if (DEBUG) Log.v(TAG, "Splitting cycleToShow: " + currentCycle.id);

    final String newId = getNewId(user);
    if (DEBUG) Log.v(TAG, "Creating new cycleToShow: " + newId);
    Single<Cycle.Builder> cycleBuilder = cachedFirstEntryDate.zipWith(Single.just(user), new BiFunction<LocalDate, FirebaseUser, Cycle.Builder>() {
      @Override
      public Cycle.Builder apply(LocalDate entryDate, FirebaseUser user) throws Exception {
        return Cycle.builder(newId, entryDate);
      }
    }).cache();
    Single<Cycle> updatedCurrentCycle = cachedFirstEntryDate
        .flatMap(new Function<LocalDate, SingleSource<? extends Cycle>>() {
          @Override
          public SingleSource<? extends Cycle> apply(LocalDate entryDate) throws Exception {
            Cycle updatedCurrentCycle = new Cycle(currentCycle);
            if (DEBUG) Log.v(TAG, "Updating " + currentCycle.id + "'s next to " + newId);
            Map<String, Object> updates = new HashMap<>();
            updates.put("next-cycleToShow-id", newId);
            updatedCurrentCycle.nextCycleId = newId;
            LocalDate endDate = entryDate.minusDays(1);
            updates.put("end-date", DateUtil.toWireStr(endDate));
            updatedCurrentCycle.endDate = endDate;
            return RxFirebaseDatabase.updateChildren(reference(user, currentCycle.id), updates)
                .andThen(Single.just(updatedCurrentCycle));
          }
        });
    Maybe<Cycle> updatedNextCycle = getCycle(user.getUid(), currentCycle.nextCycleId)
        .flatMap(new Function<Cycle, MaybeSource<Cycle>>() {
          @Override
          public MaybeSource<Cycle> apply(Cycle nextCycle) throws Exception {
            if (DEBUG) Log.v(TAG, "Updating " + nextCycle.id + "'s previous to " + newId);
            nextCycle.previousCycleId = newId;
            return RxFirebaseDatabase.setValue(reference(user, nextCycle.id).child("previous-cycleToShow-id"), newId)
                .andThen(Maybe.just(nextCycle));
          }
        });
    return Maybe
        .zip(cycleBuilder.toMaybe(), updatedNextCycle, new BiFunction<Cycle.Builder, Cycle, Cycle.Builder>() {
          @Override
          public Cycle.Builder apply(Cycle.Builder builder, Cycle nextCycle) throws Exception {
            builder.nextCycle = nextCycle;
            return builder;
          }
        })
        .switchIfEmpty(cycleBuilder.toMaybe())
        .zipWith(updatedCurrentCycle.toMaybe(), new BiFunction<Cycle.Builder, Cycle, Cycle.Builder>() {
          @Override
          public Cycle.Builder apply(Cycle.Builder builder, Cycle currentCycle) throws Exception {
            updateConsumer.accept("Creating new cycle");
            builder.previousCycle = currentCycle;
            return builder;
          }
        }).toSingle()
        .flatMap(new Function<Cycle.Builder, SingleSource<EntrySaveResult>>() {
          @Override
          public SingleSource<EntrySaveResult> apply(Cycle.Builder builder) throws Exception {
            final Cycle newCycle = builder.build();
            Completable moveEntries = chartEntryProvider.moveEntries(currentCycle, newCycle, new Predicate<LocalDate>() {
              @Override
              public boolean apply(@Nullable LocalDate entryDate) {
                return entryDate.equals(newCycle.startDate) || entryDate.isAfter(newCycle.startDate);
              }
            });
            moveEntries.doOnSubscribe(new Consumer<Disposable>() {
              @Override
              public void accept(Disposable disposable) throws Exception {
                updateConsumer.accept("Moving entries");
              }
            });
            moveEntries.doOnComplete(new Action() {
              @Override
              public void run() throws Exception {
                updateConsumer.accept("New entry saved");
              }
            });
            EntrySaveResult result = EntrySaveResult.forCycle(newCycle);
            result.newCycles.add(newCycle);
            result.changedCycles.add(builder.previousCycle);
            if (builder.nextCycle != null) {
              result.changedCycles.add(builder.nextCycle);
            }
            updateConsumer.accept("Saving new cycle");
            return putCycleRx(user.getUid(), newCycle).andThen(moveEntries).andThen(Single.just(result));
          }
        });
  }

  @Deprecated
  private DatabaseReference reference(String userId, String cycleId) {
    return reference(userId).child(cycleId);
  }

  private DatabaseReference reference(FirebaseUser user, String cycleId) {
    return reference(user).child(cycleId);
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

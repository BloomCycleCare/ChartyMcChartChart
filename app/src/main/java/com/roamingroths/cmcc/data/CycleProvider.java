package com.roamingroths.cmcc.data;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
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

  public Observable<UpdateHandle> putCycleDeferred(final FirebaseUser user, final Cycle cycle) {
    return cycleKeyProvider.putKeys(cycle, user).flatMapObservable(new Function<UpdateHandle, ObservableSource<? extends UpdateHandle>>() {
      @Override
      public ObservableSource<? extends UpdateHandle> apply(UpdateHandle putKeysHandle) throws Exception {
        List<UpdateHandle> handles = new ArrayList<>();

        UpdateHandle handle = new UpdateHandle();
        String basePath = String.format("/cycles/%s/%s/", user.getUid(), cycle.id);
        handle.updates.put(basePath + "start-date", DateUtil.toWireStr(cycle.startDate));
        handle.updates.put(basePath + "end-date", DateUtil.toWireStr(cycle.endDate));
        handle.actions.add(new Action() {
          @Override
          public void run() throws Exception {
            mCycleCache.put(cycle.id, cycle);
          }
        });
        handles.add(handle);

        handles.add(putKeysHandle);
        return Observable.fromIterable(handles);
      }
    });
  }

  @Deprecated
  public Completable putCycleRx(final String userId, final Cycle cycle) {
    Map<String, Object> updates = new HashMap<>();
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
            if (DEBUG) Log.v(TAG, "Found data for cycle: " + snapshot.getKey());
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

  public Single<UpdateHandle> dropCycle(final Cycle cycle, final FirebaseUser user) {
    return chartEntryProvider.dropEntries(cycle).map(new Function<UpdateHandle, UpdateHandle>() {
      @Override
      public UpdateHandle apply(UpdateHandle dropEntriesHandle) throws Exception {
        UpdateHandle handle = new UpdateHandle();
        handle.merge(dropEntriesHandle);
        handle.merge(cycleKeyProvider.dropKeysForCycle(cycle));
        handle.updates.put(String.format("/cycles/%s/%s", user.getUid(), cycle.id), null);
        handle.actions.add(new Action() {
          @Override
          public void run() throws Exception {
            mCycleCache.remove(cycle.id);
          }
        });
        return handle;
      }
    });
  }

  private Maybe<Cycle> getPreviousCycle(FirebaseUser user, final Cycle cycle) {
    return getAllCycles(user)
        .filter(new io.reactivex.functions.Predicate<Cycle>() {
          @Override
          public boolean test(Cycle c) throws Exception {
            if (c.endDate == null) {
              return false;
            }
            return c.endDate.plusDays(1).isEqual(cycle.startDate);
          }
        }).firstElement();
  }

  public Single<Boolean> hasPreviousCycle(FirebaseUser user, Cycle cycle) {
    return getPreviousCycle(user, cycle)
        .map(Functions.justFunction(true))
        .switchIfEmpty(Maybe.just(false))
        .toSingle();
  }

  public Single<EntrySaveResult> combineCycleRx(
      final FirebaseUser user,
      final Cycle currentCycle,
      final Consumer<String> updateConsumer) {
    Single<Cycle> previousCycle = getPreviousCycle(user, currentCycle).toSingle().cache();
    Single<UpdateHandle> dropCurrentCycleHandle = dropCycle(currentCycle, user);
    Single<UpdateHandle> copyEntriesHandle = previousCycle.flatMap(new Function<Cycle, SingleSource<? extends UpdateHandle>>() {
      @Override
      public SingleSource<? extends UpdateHandle> apply(Cycle previousCycle) throws Exception {
        return chartEntryProvider.copyEntries(currentCycle, previousCycle, Predicates.<LocalDate>alwaysTrue());
      }
    });
    Single<Cycle> updatedPreviousCycle = previousCycle.map(new Function<Cycle, Cycle>() {
      @Override
      public Cycle apply(Cycle cycle) throws Exception {
        cycle.endDate = currentCycle.endDate;
        return cycle;
      }
    }).cache();
    Single<UpdateHandle> updatePreviousCycleHandle = updatedPreviousCycle.map(new Function<Cycle, UpdateHandle>() {
      @Override
      public UpdateHandle apply(final Cycle cycle) throws Exception {
        UpdateHandle handle = new UpdateHandle();
        handle.updates.put(String.format("/cycles/%s/%s/end-date", user.getUid(), cycle.id), DateUtil.toWireStr(currentCycle.endDate));
        handle.actions.add(new Action() {
          @Override
          public void run() throws Exception {
            mCycleCache.put(cycle.id, cycle);
          }
        });
        return handle;
      }
    });
    Single<UpdateHandle> updates = UpdateHandle.merge(copyEntriesHandle, dropCurrentCycleHandle, updatePreviousCycleHandle);
    Single<EntrySaveResult> result = updatedPreviousCycle.map(new Function<Cycle, EntrySaveResult>() {
      @Override
      public EntrySaveResult apply(Cycle previousCycle) throws Exception {
        EntrySaveResult result = EntrySaveResult.forCycle(previousCycle);
        result.droppedCycles.add(currentCycle);
        result.changedCycles.add(previousCycle);
        return result;
      }
    });
    return UpdateHandle.run(updates, db.getReference()).andThen(result);
  }

  public Single<EntrySaveResult> splitCycleRx(
      final FirebaseUser user,
      final Cycle currentCycle,
      Single<LocalDate> firstEntryDate,
      final Consumer<String> updateConsumer) {
    if (DEBUG) Log.v(TAG, "Splitting cycleToShow: " + currentCycle.id);
    Single<LocalDate> cachedFirstEntryDate = firstEntryDate.cache();
    Single<Cycle> newCycle = cachedFirstEntryDate.map(new Function<LocalDate, Cycle>() {
      @Override
      public Cycle apply(LocalDate startDate) throws Exception {
        return Cycle.builder(getNewId(user), startDate).build();
      }
    }).cache();
    Single<Cycle> updatedCurrentCycle = Single.zip(Single.just(currentCycle), cachedFirstEntryDate, new BiFunction<Cycle, LocalDate, Cycle>() {
      @Override
      public Cycle apply(Cycle currentCycle, LocalDate newCycleStartDate) throws Exception {
        final Cycle updatedCurrentCycle = new Cycle(currentCycle);
        LocalDate endDate = newCycleStartDate.minusDays(1);
        updatedCurrentCycle.endDate = endDate;
        return updatedCurrentCycle;
      }
    }).cache();
    Observable<UpdateHandle> newCycleHandles = newCycle.flatMapObservable(new Function<Cycle, ObservableSource<? extends UpdateHandle>>() {
      @Override
      public ObservableSource<? extends UpdateHandle> apply(Cycle newCycle) throws Exception {
        return putCycleDeferred(user, newCycle);
      }
    }).cache();
    Single<UpdateHandle> newCyclePutHandle = newCycleHandles.firstOrError();
    Single<UpdateHandle> newCyclePutKeysHandle = newCycleHandles.skip(1).firstOrError();
    Single<UpdateHandle> updateCurrentCycleHandle = updatedCurrentCycle.map(new Function<Cycle, UpdateHandle>() {
      @Override
      public UpdateHandle apply(final Cycle updatedCurrentCycle) throws Exception {
        UpdateHandle handle = new UpdateHandle();
        handle.updates.put(
            String.format("/cycles/%s/%s/end-date", user.getUid(), updatedCurrentCycle.id),
            DateUtil.toWireStr(updatedCurrentCycle.endDate));
        handle.actions.add(new Action() {
          @Override
          public void run() throws Exception {
            mCycleCache.put(updatedCurrentCycle.id, updatedCurrentCycle);
          }
        });
        return handle;
      }
    });
    Single<UpdateHandle> moveEntriesHandle = newCycle.flatMap(new Function<Cycle, SingleSource<? extends UpdateHandle>>() {
      @Override
      public SingleSource<? extends UpdateHandle> apply(final Cycle newCycle) throws Exception {
        Predicate<LocalDate> datePredicate = new Predicate<LocalDate>() {
          @Override
          public boolean apply(@Nullable LocalDate entryDate) {
            return entryDate.equals(newCycle.startDate) || entryDate.isAfter(newCycle.startDate);
          }
        };
        Single<UpdateHandle> copyHandle = chartEntryProvider.copyEntries(currentCycle, newCycle, datePredicate);
        Single<UpdateHandle> dropHandle = chartEntryProvider.dropEntries(currentCycle, datePredicate);
        return UpdateHandle.merge(copyHandle, dropHandle);
      }
    });
    Single<UpdateHandle> updates = UpdateHandle.merge(newCyclePutKeysHandle, updateCurrentCycleHandle, moveEntriesHandle);
    Single<EntrySaveResult> result = Single.zip(newCycle, updatedCurrentCycle, new BiFunction<Cycle, Cycle, EntrySaveResult>() {
      @Override
      public EntrySaveResult apply(Cycle newCycle, Cycle updatedCurrentCycle) throws Exception {
        EntrySaveResult result = EntrySaveResult.forCycle(newCycle);
        result.newCycles.add(newCycle);
        result.changedCycles.add(currentCycle);
        return result;
      }
    });
    return UpdateHandle.run(newCyclePutHandle, db.getReference())
        .andThen(UpdateHandle.run(updates, db.getReference()))
        .andThen(result);
  }

  @Deprecated
  private DatabaseReference reference(String userId, String cycleId) {
    return reference(userId).child(cycleId);
  }

  private DatabaseReference reference(FirebaseUser user, String cycleId) {
    return reference(user).child(cycleId);
  }

  private DatabaseReference reference(FirebaseUser user, Cycle cycle) {
    return reference(user).child(cycle.id);
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

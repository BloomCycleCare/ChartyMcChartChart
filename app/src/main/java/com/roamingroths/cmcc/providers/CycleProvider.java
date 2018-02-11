package com.roamingroths.cmcc.providers;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.logic.AppState;
import com.roamingroths.cmcc.logic.chart.Cycle;
import com.roamingroths.cmcc.ui.entry.detail.EntrySaveResult;
import com.roamingroths.cmcc.utils.DateUtil;
import com.roamingroths.cmcc.utils.UpdateHandle;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collection;
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
  private final KeyProvider keyProvider;
  private final ChartEntryProvider chartEntryProvider;
  private final Map<String, Cycle> mCycleCache;

  public CycleProvider(
      FirebaseDatabase db, KeyProvider keyProvider, ChartEntryProvider chartEntryProvider) {
    this.db = db;
    this.keyProvider = keyProvider;
    this.chartEntryProvider = chartEntryProvider;
    this.mCycleCache = Maps.newConcurrentMap();
  }

  public Completable initCache(final FirebaseUser user) {
    if (DEBUG) Log.v(TAG, "Initializing cycle cache.");
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

  public Completable putCycleDatas(FirebaseUser currentUser, final Collection<AppState.CycleData> cycleDatas) {
    // This should be done better...
    List<Single<UpdateHandle>> cyclePuts = new ArrayList<>();
    List<Single<UpdateHandle>> keyPuts = new ArrayList<>();
    List<Single<UpdateHandle>> entryPuts = new ArrayList<>();
    for (AppState.CycleData cycleData : cycleDatas) {
      Observable<UpdateHandle> handles = putCycle(currentUser, cycleData.cycle).cache();
      cyclePuts.add(handles.firstOrError());
      keyPuts.add(handles.skip(1).firstOrError());
      entryPuts.add(chartEntryProvider.putCycleData(cycleData));
    }
    Completable putCycles = Single.merge(cyclePuts)
        .collectInto(UpdateHandle.forDb(db), UpdateHandle.collector())
        .flatMapCompletable(UpdateHandle.run());
    Completable putKeys = Single.merge(keyPuts)
        .collectInto(UpdateHandle.forDb(db), UpdateHandle.collector())
        .flatMapCompletable(UpdateHandle.run());
    Completable putEntries = Single.merge(entryPuts)
        .collectInto(UpdateHandle.forDb(db), UpdateHandle.collector())
        .flatMapCompletable(UpdateHandle.run());
    return putCycles.andThen(putKeys).andThen(putEntries);
  }

  private Observable<UpdateHandle> putCycle(final FirebaseUser user, final Cycle cycle) {
    return keyProvider.putCycleKeys(cycle).flatMapObservable(new Function<UpdateHandle, ObservableSource<? extends UpdateHandle>>() {
      @Override
      public ObservableSource<? extends UpdateHandle> apply(UpdateHandle putKeysHandle) throws Exception {
        UpdateHandle handle = UpdateHandle.forDb(db);
        String basePath = String.format("/cycles/%s/%s/", user.getUid(), cycle.id);
        handle.updates.put(basePath + "start-date", DateUtil.toWireStr(cycle.startDate));
        handle.updates.put(basePath + "end-date", DateUtil.toWireStr(cycle.endDate));
        handle.actions.add(new Action() {
          @Override
          public void run() throws Exception {
            mCycleCache.put(cycle.id, cycle);
          }
        });

        List<UpdateHandle> handles = new ArrayList<>();
        handles.add(handle);
        handles.add(putKeysHandle);
        return Observable.fromIterable(handles);
      }
    });
  }

  @Deprecated
  public Completable putCycleRx(final FirebaseUser user, final Cycle cycle) {
    return putCycle(user, cycle).flatMapCompletable(UpdateHandle.run());
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
            return putCycleRx(user, cycle).andThen(Maybe.just(cycle)).doOnSuccess(new Consumer<Cycle>() {
              @Override
              public void accept(Cycle cycle) throws Exception {
                mCycleCache.put(cycle.id, cycle);
              }
            });
          }
        }))
        .toSingle();
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
            return keyProvider.getCycleKeys(snapshot.getKey()).map(Cycle.fromSnapshot(snapshot)).toObservable();
          }
        });
  }

  public Single<UpdateHandle> dropCycles(final FirebaseUser user) {
    return getAllCycles(user)
        .flatMapSingle(new Function<Cycle, SingleSource<UpdateHandle>>() {
          @Override
          public SingleSource<UpdateHandle> apply(Cycle cycle) throws Exception {
            return dropCycle(cycle, user);
          }
        })
        .collectInto(UpdateHandle.forDb(db), UpdateHandle.collector());
  }

  private Single<UpdateHandle> dropCycle(final Cycle cycle, final FirebaseUser user) {
    return chartEntryProvider.dropEntries(cycle).map(new Function<UpdateHandle, UpdateHandle>() {
      @Override
      public UpdateHandle apply(UpdateHandle dropEntriesHandle) throws Exception {
        UpdateHandle handle = UpdateHandle.forDb(db);
        handle.merge(dropEntriesHandle);
        handle.merge(keyProvider.dropCycleKeys(cycle));
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
        UpdateHandle handle = UpdateHandle.forDb(db);
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
    Single<UpdateHandle> updates = Single
        .concat(Lists.newArrayList(copyEntriesHandle, dropCurrentCycleHandle, updatePreviousCycleHandle))
        .collectInto(UpdateHandle.forDb(db), UpdateHandle.collector());
    Single<EntrySaveResult> result = updatedPreviousCycle.map(new Function<Cycle, EntrySaveResult>() {
      @Override
      public EntrySaveResult apply(Cycle previousCycle) throws Exception {
        EntrySaveResult result = EntrySaveResult.forCycle(previousCycle);
        result.droppedCycles.add(currentCycle);
        result.changedCycles.add(previousCycle);
        return result;
      }
    });
    return updates.flatMapCompletable(UpdateHandle.run()).andThen(result);
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
        return putCycle(user, newCycle);
      }
    }).cache();
    Completable putNewCycle = newCycleHandles.firstOrError().flatMapCompletable(UpdateHandle.run());
    Single<UpdateHandle> newCyclePutKeysHandle = newCycleHandles.skip(1).firstOrError();
    Single<UpdateHandle> updateCurrentCycleHandle = updatedCurrentCycle.map(new Function<Cycle, UpdateHandle>() {
      @Override
      public UpdateHandle apply(final Cycle updatedCurrentCycle) throws Exception {
        UpdateHandle handle = UpdateHandle.forDb(db);
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
        return Single.concatArray(copyHandle, dropHandle).collectInto(UpdateHandle.forDb(db), UpdateHandle.collector());
      }
    });
    Completable doUpdates = Single
        .concat(Lists.newArrayList(newCyclePutKeysHandle, updateCurrentCycleHandle, moveEntriesHandle))
        .collectInto(UpdateHandle.forDb(db), UpdateHandle.collector())
        .flatMapCompletable(UpdateHandle.run());
    Single<EntrySaveResult> result = Single.zip(newCycle, updatedCurrentCycle, new BiFunction<Cycle, Cycle, EntrySaveResult>() {
      @Override
      public EntrySaveResult apply(Cycle newCycle, Cycle updatedCurrentCycle) throws Exception {
        EntrySaveResult result = EntrySaveResult.forCycle(newCycle);
        result.newCycles.add(newCycle);
        result.changedCycles.add(currentCycle);
        return result;
      }
    });
    return putNewCycle.andThen(doUpdates).andThen(result);
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

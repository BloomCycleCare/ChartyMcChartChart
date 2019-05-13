package com.roamingroths.cmcc.providers;

import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.roamingroths.cmcc.data.db.AppDatabase;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.data.repos.CycleRepo;
import com.roamingroths.cmcc.ui.entry.EntrySaveResult;

import org.joda.time.LocalDate;

import java.util.ArrayList;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.internal.functions.Functions;

/**
 * Created by parkeroth on 9/2/17.
 */
public class CycleProvider {

  // TODO: Remove FirebaseAuth stuff

  private static final boolean DEBUG = true;
  private static final String TAG = CycleProvider.class.getSimpleName();

  private final FirebaseDatabase db;
  private final CycleRepo cycleRepo;
  private final ChartEntryProvider chartEntryProvider;

  public CycleProvider(
      FirebaseDatabase db, AppDatabase localDB, ChartEntryProvider chartEntryProvider) {
    this.db = db;
    this.cycleRepo = new CycleRepo(localDB);
    this.chartEntryProvider = chartEntryProvider;
  }

  private String getNewId(FirebaseUser user) {
    return reference(user).push().getKey();
  }

  public Single<EntrySaveResult> createCycle(FirebaseUser user, LocalDate startDate, LocalDate endDate) {
    try {
      Cycle.Builder builder = Cycle.builder(getNewId(user), startDate);
      builder.endDate = endDate;
      Cycle newCycle = builder.build();
      EntrySaveResult result = EntrySaveResult.forCycle(newCycle);
      result.newCycles.add(newCycle);
      return cycleRepo
          .insertOrUpdate(newCycle)
          .andThen(chartEntryProvider.createEmptyEntries(newCycle))
          .andThen(Single.just(result));
    } catch (Exception e) {
      return Single.error(e);
    }
  }

  public Observable<Cycle> getAll() {
    return cycleRepo.getStream().first(new ArrayList<>()).flatMapObservable(Observable::fromIterable);
  }

  private Maybe<Cycle> getPreviousCycle(final Cycle cycle) {
    return getAll()
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

  public Single<Boolean> hasPreviousCycle(Cycle cycle) {
    return getPreviousCycle(cycle)
        .map(Functions.justFunction(true))
        .switchIfEmpty(Maybe.just(false))
        .toSingle();
  }

  /*public Single<EntrySaveResult> combineCycleRx(
      final FirebaseUser user,
      final Cycle currentCycle,
      final Consumer<String> updateConsumer) {
    Single<Cycle> previousCycle = getPreviousCycle(currentCycle).toSingle().cache();

    Single<UpdateHandle> dropCurrentCycleHandle = dropCycle(currentCycle, user);

    Single<UpdateHandle> copyEntriesHandle = previousCycle.flatMap(previousCycle1 -> chartEntryProvider.copyEntries(currentCycle, previousCycle1, Predicates.<LocalDate>alwaysTrue()));

    Single<Cycle> updatedPreviousCycle = previousCycle
        .map(cycle -> {
          cycle.endDate = currentCycle.endDate;
          return cycle;
        })
        .flatMap(updatedCycle -> cycleDao
            .insert(updatedCycle)
            .andThen(Single.just(updatedCycle)))
        .cache();

    Single<UpdateHandle> updatePreviousCycleHandle = updatedPreviousCycle.map(cycle -> {
      UpdateHandle handle = UpdateHandle.forDb(db);
      handle.updates.put(String.format("/cycles/%s/%s/end-date", user.getUid(), cycle.id), DateUtil.toWireStr(currentCycle.endDate));
      return handle;
    });

    Single<UpdateHandle> updates = Single
        .concat(Lists.newArrayList(copyEntriesHandle, dropCurrentCycleHandle, updatePreviousCycleHandle))
        .collectInto(UpdateHandle.forDb(db), UpdateHandle.collector());

    Single<EntrySaveResult> result = updatedPreviousCycle.map(previousCycle12 -> {
      EntrySaveResult result1 = EntrySaveResult.forCycle(previousCycle12);
      result1.droppedCycles.add(currentCycle);
      result1.changedCycles.add(previousCycle12);
      return result1;
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
    Single<Cycle> newCycle = cachedFirstEntryDate.map(startDate -> Cycle.builder(getNewId(user), startDate).build()).cache();

    Single<Cycle> updatedCurrentCycle = Single.zip(
        Single.just(currentCycle),
        cachedFirstEntryDate,
        (currentCycle1, newCycleStartDate) -> {
          final Cycle updatedCurrentCycle1 = new Cycle(currentCycle1);
          LocalDate endDate = newCycleStartDate.minusDays(1);
          updatedCurrentCycle1.endDate = endDate;
          return updatedCurrentCycle1;
        })
        .flatMap(updatedCycle -> cycleDao
            .insert(updatedCycle)
            .andThen(Single.just(updatedCycle)))
        .cache();

    Observable<UpdateHandle> newCycleHandles = newCycle.flatMapObservable(newCycle1 -> putCycle(user, newCycle1)).cache();

    Completable putNewCycle = newCycleHandles.firstOrError().flatMapCompletable(UpdateHandle.run());

    Single<UpdateHandle> newCyclePutKeysHandle = newCycleHandles.skip(1).firstOrError();

    Single<UpdateHandle> updateCurrentCycleHandle = updatedCurrentCycle.map(updatedCurrentCycle12 -> {
      UpdateHandle handle = UpdateHandle.forDb(db);
      handle.updates.put(
          String.format("/cycles/%s/%s/end-date", user.getUid(), updatedCurrentCycle12.id),
          DateUtil.toWireStr(updatedCurrentCycle12.endDate));
      return handle;
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
  }*/

  @Deprecated
  private DatabaseReference reference(String userId) {
    return db.getReference("cycles").child(userId);
  }

  private DatabaseReference reference(FirebaseUser user) {
    return db.getReference("cycles").child(user.getUid());
  }

  private void logV(String message) {
    if (DEBUG) Log.v(TAG, String.format("%s: %s", Thread.currentThread().getName(), message));
  }

  private void logV(Cycle cycle, String message) {
    if (DEBUG) Log.v(TAG, String.format("%s: %s: %s", Thread.currentThread().getName(), cycle.id, message));
  }
}

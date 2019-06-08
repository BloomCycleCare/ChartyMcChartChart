package com.roamingroths.cmcc.data.repos;

import androidx.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.db.GenericDao;
import com.roamingroths.cmcc.data.db.InstructionDao;
import com.roamingroths.cmcc.data.entities.Instructions;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import timber.log.Timber;

public class InstructionsRepo {

  private final InstructionDao mInstructionDao;
  private final TempStore<Instructions, LocalDate> mTempStore;

  public InstructionsRepo(MyApplication myApp) {
    mInstructionDao = myApp.db().instructionDao();
    mTempStore = new TempStore<>(mInstructionDao.getAll().toObservable(), instruction -> instruction.startDate);
  }

  public Flowable<Instructions> get(LocalDate startDate) {
    return mTempStore.get(startDate);
  }

  public Flowable<List<Instructions>> getAll() {
    return mTempStore.getStream();
  }

  public Maybe<Instructions> getCurrent() {
    return mTempStore.getStream()
        .firstOrError()
        .flatMapMaybe(instructions -> {
          Instructions currentInstructions = null;
          for (Instructions i : instructions) {
            if (currentInstructions == null || i.startDate.isAfter(currentInstructions.startDate)) {
              currentInstructions = i;
            }
          }
          return currentInstructions != null ? Maybe.just(currentInstructions) : Maybe.empty();
        });
  }

  public Single<Boolean> hasAnyAfter(LocalDate date) {
    return getAll()
        .firstOrError()
        .map(instructionsList -> {
          for (Instructions i : instructionsList) {
            if (i.startDate.isAfter(date)) {
              return true;
            }
          }
          return false;
        });
  }

  public Maybe<Instructions> getActiveInstructions(LocalDate date) {
    return getAll()
        .firstOrError()
        .flatMapMaybe(instructionsList -> {
          Instructions nearestInstructions = null;
          for (Instructions i : instructionsList) {
            if (i.startDate.isAfter(date)) {
              continue;
            }
            if (nearestInstructions == null || i.startDate.isBefore(nearestInstructions.startDate)) {
              nearestInstructions = i;
            }
          }
          return nearestInstructions == null ? Maybe.empty() : Maybe.just(nearestInstructions);
        });
  }

  public Single<Instructions> delete(Instructions instructions) {
    Timber.i("Deleting instructions starting on %s", instructions.startDate);
    return mTempStore
        .delete(instructions)
        .andThen(getActiveInstructions(instructions.startDate)
            .switchIfEmpty(getCurrent().toSingle())
            .doOnError(t -> Timber.e("No active instructions!")));
  }

  public Completable insertOrUpdate(Instructions instructions) {
    return mTempStore.updateOrInsert(instructions);
  }

  public Single<Boolean> isDirty() {
    return mTempStore.isDirty();
  }

  public Completable commit() {
    return mTempStore.commit(mInstructionDao).subscribeOn(Schedulers.computation());
  }

  public Completable clearPending() {
    return mTempStore.clearPending().subscribeOn(Schedulers.computation());
  }

  private static class TempStore<T, P extends Comparable<? super P>> {

    private final Function<T, P> mPrimaryKeyExtractor;
    private final BehaviorSubject<List<T>> mUpdatedValues = BehaviorSubject.create();
    private final BehaviorSubject<List<T>> mStoredValues = BehaviorSubject.create();

    TempStore(Observable<List<T>> remoteValues, Function<T, P> primaryKeyExtractor) {
      mPrimaryKeyExtractor = primaryKeyExtractor;
      remoteValues.subscribe(mStoredValues);
      mStoredValues.firstOrError().subscribe(mUpdatedValues::onNext, t -> Timber.e(t));
    }

    public Flowable<List<T>> getStream() {
      return mUpdatedValues.toFlowable(BackpressureStrategy.BUFFER).map(entries -> {
        List<T> out = new ArrayList<>(entries);
        Collections.sort(out, (a, b) -> mPrimaryKeyExtractor.apply(a).compareTo(mPrimaryKeyExtractor.apply(b)));
        return ImmutableList.copyOf(out);
      });
    }

    public Flowable<T> get(P primaryKey) {
      return getStream()
          .switchMap(values -> {
            for (T value : values) {
              if (mPrimaryKeyExtractor.apply(value).equals(primaryKey)) {
                return Flowable.just(value);
              }
            }
            return Flowable.empty();
          })
          .distinctUntilChanged()
          .doOnNext(t -> Timber.v("New value for %s", primaryKey))
          .doOnSubscribe(s -> Timber.v("Subscribe for %s", primaryKey))
          .doOnComplete(() -> Timber.v("Complete for %s", primaryKey))
          .doOnTerminate(() -> Timber.v("Terminate for %s", primaryKey));
    }

    public Completable insert(T value) {
      P primaryKey = mPrimaryKeyExtractor.apply(value);
      return mUpdatedValues
          .firstOrError()
          .map(updatedValues -> {
            ImmutableList.Builder<T> out = new ImmutableList.Builder<>();
            for (T t : updatedValues) {
              if (primaryKey.equals(mPrimaryKeyExtractor.apply(t))) {
                throw new IllegalArgumentException(String.format("Value already exists for %s", primaryKey));
              }
              out.add(t);
            }
            out.add(value);
            return out.build();
          })
          .flatMapCompletable(out -> {
            mUpdatedValues.onNext(out);
            return Completable.complete();
          });
    }

    public Completable updateOrInsert(T value) {
      P primaryKey = mPrimaryKeyExtractor.apply(value);
      return mUpdatedValues
          .firstOrError()
          .map(updatedValues -> {
            ImmutableList.Builder<T> out = new ImmutableList.Builder<>();
            for (T t : updatedValues) {
              if (primaryKey.equals(mPrimaryKeyExtractor.apply(t))) {
                continue;
              }
              out.add(t);
            }
            out.add(value);
            return out.build();
          })
          .flatMapCompletable(out -> {
            mUpdatedValues.onNext(out);
            return Completable.complete();
          });
    }

    public Single<Boolean> isDirty() {
      return Single.zip(
          mStoredValues.firstOrError(),
          mUpdatedValues.firstOrError(),
          (storedValues, updatedValues) -> {
            Set<T> tmp = new HashSet<>(updatedValues);
            Map<P, T> index = new HashMap<>();
            for (T updatedValue : tmp) {
              index.put(mPrimaryKeyExtractor.apply(updatedValue), updatedValue);
            }
            for (T storedValue : storedValues) {
              P primaryKey = mPrimaryKeyExtractor.apply(storedValue);
              if (!index.containsKey(primaryKey)) {
                // storedValue was removed
                return true;
              }
              if (!storedValue.equals(index.get(primaryKey))) {
                // storedValue was changed
                return true;
              }
              tmp.remove(index.get(primaryKey));
              index.remove(primaryKey);
            }
            if (!tmp.isEmpty()) {
              // a new value was added
              return true;
            }
            return false;
          });
    }

    public Completable clearPending() {
      return mStoredValues.flatMapCompletable(storedValues -> {
        mUpdatedValues.onNext(storedValues);
        return Completable.complete();
      });
    }

    public Completable delete(T value) {
      P primaryKey = mPrimaryKeyExtractor.apply(value);
      return mUpdatedValues
          .firstOrError()
          .map(updatedValues -> {
            ImmutableList.Builder<T> out = new ImmutableList.Builder<>();
            for (T t : updatedValues) {
              if (!primaryKey.equals(mPrimaryKeyExtractor.apply(t))) {
                out.add(t);
              }
            }
            return out.build();
          })
          .flatMapCompletable(out -> {
            mUpdatedValues.onNext(out);
            return Completable.complete();
          });
    }

    public Completable commit(GenericDao<T> dao) {
      return isDirty().flatMapCompletable(isDirty -> {
        if (!isDirty) {
          return Completable.complete();
        }
        return mUpdatedValues.firstOrError().flatMapCompletable(dao::insert);
      });
    }
  }

  public static class Neighbours {
    @Nullable public final Instructions next;
    @Nullable public final Instructions prior;

    Neighbours(@Nullable Instructions next, @Nullable Instructions prior) {
      this.next = next;
      this.prior = prior;
    }
  }
}

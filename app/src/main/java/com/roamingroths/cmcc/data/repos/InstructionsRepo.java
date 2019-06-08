package com.roamingroths.cmcc.data.repos;

import com.roamingroths.cmcc.application.MyApplication;
import com.roamingroths.cmcc.data.db.InstructionDao;
import com.roamingroths.cmcc.data.entities.Instructions;
import com.roamingroths.cmcc.utils.TempStore;

import org.joda.time.LocalDate;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class InstructionsRepo {

  private final InstructionDao mInstructionDao;
  private final TempStore<Instructions, LocalDate> mTempStore;

  public InstructionsRepo(MyApplication myApp) {
    mInstructionDao = myApp.db().instructionDao();
    mTempStore = new TempStore<>(mInstructionDao.getStream().toObservable(), instruction -> instruction.startDate);
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
}

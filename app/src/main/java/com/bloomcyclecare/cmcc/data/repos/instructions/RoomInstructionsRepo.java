package com.bloomcyclecare.cmcc.data.repos.instructions;

import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.db.InstructionDao;
import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.bloomcyclecare.cmcc.utils.TempStore;

import org.joda.time.LocalDate;

import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;

class RoomInstructionsRepo implements RWInstructionsRepo {

  private final InstructionDao mInstructionDao;
  private final TempStore<Instructions, LocalDate> mTempStore;
  private final PublishSubject<UpdateEvent> updates = PublishSubject.create();

  RoomInstructionsRepo(AppDatabase db) {
    mInstructionDao = db.instructionDao();
    mTempStore = new TempStore<>(mInstructionDao.getStream().toObservable(), instruction -> instruction.startDate);
  }

  @Override
  public Flowable<UpdateEvent> updateEvents() {
    return updates.toFlowable(BackpressureStrategy.BUFFER);
  }

  @Override
  public Flowable<Instructions> get(LocalDate startDate) {
    return mTempStore.get(startDate);
  }

  @Override
  public Flowable<List<Instructions>> getAll() {
    return mTempStore.getStream();
  }

  @Override
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

  private Maybe<Instructions> getCurrent() {
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

  private Maybe<Instructions> getActiveInstructions(LocalDate date) {
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

  @Override
  public Single<Instructions> delete(Instructions instructions) {
    Timber.i("Deleting instructions starting on %s", instructions.startDate);
    return mTempStore
        .delete(instructions)
        .andThen(getActiveInstructions(instructions.startDate)
            .switchIfEmpty(getCurrent().toSingle())
            .doOnError(t -> Timber.e("No active instructions!")))
        .doOnSuccess(i -> updates.onNext(UpdateEvent.forInstructions(instructions)));
  }

  @Override
  public Completable insertOrUpdate(Instructions instructions) {
    return mTempStore.updateOrInsert(instructions)
        .doOnComplete(() -> updates.onNext(UpdateEvent.forInstructions(instructions)));
  }

  @Override
  public Single<Boolean> isDirty() {
    return mTempStore.isDirty();
  }

  @Override
  public Completable commit() {
    return mTempStore.commit(mInstructionDao).subscribeOn(Schedulers.computation());
  }

  @Override
  public Completable clearPending() {
    return mTempStore.clearPending().subscribeOn(Schedulers.computation());
  }
}

package com.bloomcyclecare.cmcc.data.repos.instructions;

import com.bloomcyclecare.cmcc.data.models.instructions.Instructions;
import com.google.common.collect.Range;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;

public interface RWInstructionsRepo extends ROInstructionsRepo {
  class UpdateEvent {
    public final DateTime updateTime;
    public final Range<LocalDate> dateRange;

    private UpdateEvent(DateTime updateTime, Range<LocalDate> dateRange) {
      this.updateTime = updateTime;
      this.dateRange = dateRange;
    }

    static UpdateEvent forInstructions(Instructions instructions) {
      // TODO: restrict range to when the instructions were active
      return new UpdateEvent(DateTime.now(), Range.closed(
          instructions.startDate, LocalDate.now()));
    }
  }

  Flowable<UpdateEvent> updateEvents();

  Single<Instructions> delete(Instructions instructions);

  Completable deleteAll();

  Completable insertOrUpdate(Instructions instructions);

  Single<Boolean> isDirty();

  Completable commit();

  Completable clearPending();
}

package com.bloomcyclecare.cmcc.data.repos.cycle;

import com.bloomcyclecare.cmcc.data.models.charting.Cycle;
import com.google.common.base.Optional;
import com.google.common.collect.Range;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import androidx.core.util.Consumer;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;

public interface RWCycleRepo extends ROCycleRepo {

  class SplitResult {
    public final Cycle newCycle;
    public final Cycle previousCycle;

    SplitResult(Cycle newCycle, Cycle previousCycle) {
      this.newCycle = newCycle;
      this.previousCycle = previousCycle;
    }
  }

  Single<SplitResult> splitCycle(Cycle cycleToSplit, LocalDate firstDayOfNewCycle);

  Single<SplitResult> splitCycle(Cycle cycleToSplit, LocalDate firstDayOfNewCycle, Consumer<Cycle> fieldUpdater);

  enum JoinType {
    WITH_PREVIOUS, WITH_NEXT;
  }

  Single<Cycle> joinCycle(Cycle cycleToJoin, RWCycleRepo.JoinType joinType);

  class UpdateEvent {
    public final DateTime updateTime;
    public final Range<LocalDate> dateRange;

    private UpdateEvent(DateTime updateTime, Range<LocalDate> dateRange) {
      this.updateTime = updateTime;
      this.dateRange = dateRange;
    }

    static UpdateEvent forCycle(Cycle cycle) {
      return new UpdateEvent(DateTime.now(), Range.closed(
          cycle.startDate, Optional.fromNullable(cycle.endDate).or(LocalDate.now())));
    }
  }

  Flowable<UpdateEvent> updateEvents();

  Completable deleteAll();

  Completable delete(Cycle cycle);

  Completable insertOrUpdate(Cycle cycle);
}

package com.bloomcyclecare.cmcc.data.repos.cycle;

import com.bloomcyclecare.cmcc.data.models.charting.Cycle;

import org.joda.time.LocalDate;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Maybe;

public interface ROCycleRepo {

  Flowable<List<Cycle>> getStream();

  Maybe<Cycle> getPreviousCycle(Cycle cycle);

  Maybe<Cycle> getNextCycle(Cycle cycle);

  Maybe<Cycle> getCurrentCycle();

  Maybe<Cycle> getLatestCycle();

  Maybe<Cycle> getCycleForDate(LocalDate date);
}

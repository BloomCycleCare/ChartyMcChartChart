package com.bloomcyclecare.cmcc.data.repos.entry;

import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.models.ChartEntry;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;

public interface ROChartEntryRepo {

  @Deprecated
  Flowable<List<ChartEntry>> getStream(Flowable<Cycle> cycleStream);

  Single<List<ChartEntry>> getAllEntries();

  Flowable<List<ChartEntry>> getLatestN(int n);

  Flowable<List<ChartEntry>> getStreamForCycle(Flowable<Cycle> cycleStream);
}

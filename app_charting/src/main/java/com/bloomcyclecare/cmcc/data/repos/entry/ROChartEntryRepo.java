package com.bloomcyclecare.cmcc.data.repos.entry;

import com.bloomcyclecare.cmcc.data.models.charting.Cycle;
import com.bloomcyclecare.cmcc.data.models.charting.ChartEntry;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;

public interface ROChartEntryRepo {

  Single<List<ChartEntry>> getAllEntries();

  Flowable<List<ChartEntry>> getLatestN(int n);

  Flowable<List<ChartEntry>> getStreamForCycle(Flowable<Cycle> cycleStream);
}

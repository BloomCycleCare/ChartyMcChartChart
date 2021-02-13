package com.bloomcyclecare.cmcc.data.repos.entry;

import com.bloomcyclecare.cmcc.data.models.charting.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.charting.Cycle;

import org.joda.time.LocalDate;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;

public interface ROChartEntryRepo {

  Single<List<ChartEntry>> getAllEntries();

  Flowable<List<ChartEntry>> getAllBetween(LocalDate start, LocalDate endInclusive);

  Flowable<List<ChartEntry>> getLatestN(int n);

  Flowable<List<ChartEntry>> getStreamForCycle(Flowable<Cycle> cycleStream);
}

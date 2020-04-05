package com.bloomcyclecare.cmcc.data.repos.entry;

import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.models.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.TrainingCycle;
import com.google.common.collect.ImmutableList;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import timber.log.Timber;

public class TrainingChartEntryRepo implements RWChartEntryRepo {

  private final ImmutableList<ChartEntry> mEntries;

  TrainingChartEntryRepo(List<TrainingCycle> trainingCycles) {
    ImmutableList.Builder<ChartEntry> builder = ImmutableList.builder();
    mEntries = builder.build();
  }

  @Override
  public Single<List<ChartEntry>> getAllEntries() {
    return getStreamForCycle(Flowable.empty()).firstOrError();
  }

  @Override
  public Flowable<List<ChartEntry>> getLatestN(int n) {
    return null;
  }

  @Override
  public Flowable<List<ChartEntry>> getStreamForCycle(Flowable<Cycle> cycleStream) {
    return null;
  }

  @Override
  public Flowable<UpdateEvent> updateEvents() {
    return Flowable.<UpdateEvent>empty()
        .doOnSubscribe(s -> Timber.w("No updates will be provided from training repo"));
  }

  @Override
  public Completable insert(ChartEntry entry) {
    return Completable.error(
        new UnsupportedOperationException("Updates not supported on training repo"));
  }

  @Override
  public Completable deleteAll() {
    return Completable.error(
        new UnsupportedOperationException("Updates not supported on training repo"));
  }

  @Override
  public Completable delete(ChartEntry entry) {
    return Completable.error(
        new UnsupportedOperationException("Updates not supported on training repo"));
  }
}

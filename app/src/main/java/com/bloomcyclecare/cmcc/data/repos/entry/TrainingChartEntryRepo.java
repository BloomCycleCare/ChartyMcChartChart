package com.bloomcyclecare.cmcc.data.repos.entry;

import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.entities.ObservationEntry;
import com.bloomcyclecare.cmcc.data.entities.SymptomEntry;
import com.bloomcyclecare.cmcc.data.entities.WellnessEntry;
import com.bloomcyclecare.cmcc.data.models.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.TrainingCycle;
import com.bloomcyclecare.cmcc.data.models.TrainingEntry;
import com.bloomcyclecare.cmcc.logic.chart.ObservationParser;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import timber.log.Timber;

public class TrainingChartEntryRepo implements RWChartEntryRepo {

  private final TreeSet<ChartEntry> mEntries;

  TrainingChartEntryRepo(List<TrainingCycle> trainingCycles) throws ObservationParser.InvalidObservationException {
    int numEntries = 0;
    for (TrainingCycle trainingCycle : trainingCycles) {
      numEntries += trainingCycle.entries().size();
    }
    ArrayList<ChartEntry> entries = new ArrayList<>(numEntries);
    for (TrainingCycle trainingCycle : trainingCycles) {
      for (TrainingEntry trainingEntry : trainingCycle.entries().keySet()) {
        int numEntriesRemaining = numEntries - entries.size();
        LocalDate entryDate = LocalDate.now().minusDays(numEntriesRemaining - 1);
        ObservationEntry observationEntry = trainingEntry.asChartEntry(entryDate);
        ChartEntry entry = new ChartEntry(entryDate, observationEntry, WellnessEntry.emptyEntry(entryDate), SymptomEntry.emptyEntry(entryDate));
        entry.marker = trainingEntry.marker().orElse("");
        entries.add(entry);
      }
    }
    mEntries = new TreeSet<>(entries);
  }

  @Override
  public Single<List<ChartEntry>> getAllEntries() {
    return Single.just(ImmutableList.copyOf(mEntries));
  }

  @Override
  public Flowable<List<ChartEntry>> getLatestN(int n) {
    Deque<ChartEntry> out = new ArrayDeque<>(n);
    Iterator<ChartEntry> iterator = mEntries.descendingIterator();
    while (out.size() < n && iterator.hasNext()) {
      out.push(iterator.next());
    }
    return Flowable.just(ImmutableList.copyOf(out));
  }

  @Override
  public Flowable<List<ChartEntry>> getStreamForCycle(Flowable<Cycle> cycleStream) {
    return cycleStream.map(cycle -> {
      SortedSet<ChartEntry> out = mEntries.tailSet(ChartEntry.emptyEntry(cycle.startDate));
      if (cycle.endDate != null) {
        out = out.headSet(ChartEntry.emptyEntry(cycle.endDate.plusDays(1)));
      }
      return ImmutableList.copyOf(out);
    });
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
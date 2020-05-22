package com.bloomcyclecare.cmcc.data.repos.entry;

import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.entities.ObservationEntry;
import com.bloomcyclecare.cmcc.data.entities.SymptomEntry;
import com.bloomcyclecare.cmcc.data.entities.WellnessEntry;
import com.bloomcyclecare.cmcc.data.models.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.StickerSelection;
import com.bloomcyclecare.cmcc.data.models.TrainingCycle;
import com.bloomcyclecare.cmcc.data.models.TrainingEntry;
import com.bloomcyclecare.cmcc.logic.chart.ObservationParser;
import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class TrainingChartEntryRepo implements RWChartEntryRepo {

  private final TreeMap<LocalDate, ChartEntry> mEntries = new TreeMap<>();
  private final Subject<LocalDate> mUpdateSubject = PublishSubject.create();

  TrainingChartEntryRepo(List<TrainingCycle> trainingCycles, boolean populateStickerSelections) throws ObservationParser.InvalidObservationException {
    int numEntries = 0;
    for (TrainingCycle trainingCycle : trainingCycles) {
      numEntries += trainingCycle.entries().size();
    }
    for (TrainingCycle trainingCycle : trainingCycles) {
      for (Map.Entry<TrainingEntry, Optional<TrainingCycle.StickerExpectations>> mapEntry
          : trainingCycle.entries().entrySet()) {
        TrainingEntry trainingEntry = mapEntry.getKey();
        int numEntriesRemaining = numEntries - mEntries.size();
        LocalDate entryDate = LocalDate.now().minusDays(numEntriesRemaining - 1);
        ObservationEntry observationEntry = trainingEntry.asChartEntry(entryDate);
        StickerSelection stickerSelection = mapEntry.getValue().map(StickerSelection::fromExpectations).orElse(null);
        ChartEntry entry = new ChartEntry(entryDate, observationEntry,
            WellnessEntry.emptyEntry(entryDate), SymptomEntry.emptyEntry(entryDate),
            populateStickerSelections ? stickerSelection : null);
        entry.marker = trainingEntry.marker().orElse("");
        mEntries.put(entryDate, entry);
      }
    }
  }

  @Override
  public Single<List<ChartEntry>> getAllEntries() {
    return Single.just(ImmutableList.copyOf(mEntries.values()));
  }

  @Override
  public Flowable<List<ChartEntry>> getLatestN(int n) {
    Deque<ChartEntry> out = new ArrayDeque<>(n);
    Iterator<ChartEntry> iterator = mEntries.descendingMap().values().iterator();
    while (out.size() < n && iterator.hasNext()) {
      out.push(iterator.next());
    }
    return Flowable.just(ImmutableList.copyOf(out));
  }

  @Override
  public Flowable<List<ChartEntry>> getStreamForCycle(Flowable<Cycle> cycleStream) {
    return mUpdateSubject
        .map(d -> true)
        .startWith(false)
        .toFlowable(BackpressureStrategy.BUFFER)
        .flatMap(v -> cycleStream.map(cycle -> {
          SortedMap<LocalDate, ChartEntry> m = mEntries.tailMap(cycle.startDate);
          if (cycle.endDate != null) {
            m = m.headMap(cycle.endDate.plusDays(1));
          }
          return ImmutableList.copyOf(m.values());
        }));
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

  @Override
  public boolean beginBatchUpdates() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean completeBatchUpdates() {
    throw new UnsupportedOperationException();
  }
}

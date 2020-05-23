package com.bloomcyclecare.cmcc.data.repos.entry;

import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.entities.ObservationEntry;
import com.bloomcyclecare.cmcc.data.entities.SymptomEntry;
import com.bloomcyclecare.cmcc.data.entities.WellnessEntry;
import com.bloomcyclecare.cmcc.data.models.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.StickerSelection;
import com.bloomcyclecare.cmcc.data.models.TrainingCycle;
import com.bloomcyclecare.cmcc.data.models.TrainingEntry;
import com.bloomcyclecare.cmcc.data.repos.sticker.RWStickerSelectionRepo;
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
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import timber.log.Timber;

public class TrainingChartEntryRepo implements RWChartEntryRepo {

  private final Subject<LocalDate> mUpdateSubject = PublishSubject.create();
  private final Subject<TreeMap<LocalDate, ChartEntry>> mEntriesSubject = BehaviorSubject.create();

  TrainingChartEntryRepo(List<TrainingCycle> trainingCycles, boolean populateStickerSelections, RWStickerSelectionRepo stickerSelectionRepo) throws ObservationParser.InvalidObservationException {
    int numEntries = 0;
    for (TrainingCycle trainingCycle : trainingCycles) {
      numEntries += trainingCycle.entries().size();
    }
    TreeMap<LocalDate, ChartEntry> initialEntries = new TreeMap<>();
    for (TrainingCycle trainingCycle : trainingCycles) {
      for (Map.Entry<TrainingEntry, Optional<TrainingCycle.StickerExpectations>> mapEntry
          : trainingCycle.entries().entrySet()) {
        TrainingEntry trainingEntry = mapEntry.getKey();
        int numEntriesRemaining = numEntries - initialEntries.size();
        LocalDate entryDate = LocalDate.now().minusDays(numEntriesRemaining - 1);
        ObservationEntry observationEntry = trainingEntry.asChartEntry(entryDate);
        StickerSelection stickerSelection = mapEntry.getValue().map(StickerSelection::fromExpectations).orElse(null);
        ChartEntry entry = new ChartEntry(entryDate, observationEntry,
            WellnessEntry.emptyEntry(entryDate), SymptomEntry.emptyEntry(entryDate),
            populateStickerSelections ? stickerSelection : null);
        entry.marker = trainingEntry.marker().orElse("");
        initialEntries.put(entryDate, entry);
      }
    }
    stickerSelectionRepo.getSelections().scan(initialEntries, (entries, selections) -> {
      TreeMap<LocalDate, ChartEntry> copy = new TreeMap<>(entries);
      for (Map.Entry<LocalDate, ChartEntry> entry : copy.entrySet()) {
        entry.getValue().stickerSelection = selections.get(entry.getKey());
      }
      return copy;
    }).toObservable().subscribe(mEntriesSubject);
  }

  @Override
  public Single<List<ChartEntry>> getAllEntries() {
    return mEntriesSubject.map(TrainingChartEntryRepo::valuesAsList).firstOrError();
  }

  @Override
  public Flowable<List<ChartEntry>> getLatestN(int n) {
    return mEntriesSubject.map(entries -> {
      Deque<ChartEntry> out = new ArrayDeque<>(n);
      Iterator<ChartEntry> iterator = entries.descendingMap().values().iterator();
      while (out.size() < n && iterator.hasNext()) {
        out.push(iterator.next());
      }
      List<ChartEntry> l = ImmutableList.copyOf(out);
      return l;
    }).toFlowable(BackpressureStrategy.BUFFER);
  }

  private static <T> List<T> valuesAsList(Map<?, T> m) {
    List<T> l = ImmutableList.copyOf(m.values());
    return l;
  }

  @Override
  public Flowable<List<ChartEntry>> getStreamForCycle(Flowable<Cycle> cycleStream) {
    return Flowable.combineLatest(
        mEntriesSubject.toFlowable(BackpressureStrategy.BUFFER),
        cycleStream,
        (entries, cycle) -> {
          SortedMap<LocalDate, ChartEntry> m = entries.tailMap(cycle.startDate);
          if (cycle.endDate != null) {
            m = m.headMap(cycle.endDate.plusDays(1));
          }
          return ImmutableList.copyOf(m.values());
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

  @Override
  public boolean beginBatchUpdates() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean completeBatchUpdates() {
    throw new UnsupportedOperationException();
  }
}

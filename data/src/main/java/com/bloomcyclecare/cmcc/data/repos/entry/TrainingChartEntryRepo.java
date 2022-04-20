package com.bloomcyclecare.cmcc.data.repos.entry;

import com.bloomcyclecare.cmcc.data.models.breastfeeding.BreastfeedingEntry;
import com.bloomcyclecare.cmcc.data.models.charting.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.charting.Cycle;
import com.bloomcyclecare.cmcc.data.models.lifestyle.LifestyleEntry;
import com.bloomcyclecare.cmcc.data.models.measurement.MeasurementEntry;
import com.bloomcyclecare.cmcc.data.models.observation.Observation;
import com.bloomcyclecare.cmcc.data.models.observation.ObservationEntry;
import com.bloomcyclecare.cmcc.data.models.observation.SymptomEntry;
import com.bloomcyclecare.cmcc.data.models.observation.WellnessEntry;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelection;
import com.bloomcyclecare.cmcc.data.models.training.StickerExpectations;
import com.bloomcyclecare.cmcc.data.models.training.TrainingCycle;
import com.bloomcyclecare.cmcc.data.models.training.TrainingEntry;
import com.bloomcyclecare.cmcc.data.repos.sticker.RWStickerSelectionRepo;
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
import java.util.function.Function;

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

  TrainingChartEntryRepo(List<TrainingCycle> trainingCycles,
                         boolean populateStickerSelections,
                         RWStickerSelectionRepo stickerSelectionRepo,
                         Function<String, Optional<Observation>> observationParser) {
    int numEntries = 0;
    for (TrainingCycle trainingCycle : trainingCycles) {
      numEntries += trainingCycle.entries().size();
    }
    TreeMap<LocalDate, ChartEntry> initialEntries = new TreeMap<>();
    for (TrainingCycle trainingCycle : trainingCycles) {
      for (Map.Entry<TrainingEntry, Optional<StickerExpectations>> mapEntry
          : trainingCycle.entries().entrySet()) {
        TrainingEntry trainingEntry = mapEntry.getKey();
        int numEntriesRemaining = numEntries - initialEntries.size();
        LocalDate entryDate = LocalDate.now().minusDays(numEntriesRemaining - 1);
        ObservationEntry observationEntry = trainingEntry.asChartEntry(entryDate, observationParser);
        StickerSelection stickerSelection = mapEntry.getValue().map(e -> e.stickerSelection).orElse(null);
        ChartEntry entry = new ChartEntry(entryDate, observationEntry,
            WellnessEntry.emptyEntry(entryDate), SymptomEntry.emptyEntry(entryDate),
            MeasurementEntry.emptyEntry(entryDate),
            BreastfeedingEntry.emptyEntry(entryDate),
            LifestyleEntry.emptyEntry(entryDate),
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
  public Flowable<List<ChartEntry>> getAllBetween(LocalDate start, LocalDate endInclusive) {
    return mEntriesSubject.toFlowable(BackpressureStrategy.BUFFER)
        .map(entries -> {
          SortedMap<LocalDate, ChartEntry> m = entries.tailMap(start);
          m = m.headMap(endInclusive);
          return ImmutableList.copyOf(m.values());
        });
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

package com.bloomcyclecare.cmcc.data.repos.entry;

import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.db.ObservationEntryDao;
import com.bloomcyclecare.cmcc.data.db.SymptomEntryDao;
import com.bloomcyclecare.cmcc.data.db.WellnessEntryDao;
import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.entities.ObservationEntry;
import com.bloomcyclecare.cmcc.data.models.ChartEntry;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.bloomcyclecare.cmcc.utils.RxUtil;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.core.util.Pair;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;

class RoomChartEntryRepo implements RWChartEntryRepo {

  private final PublishSubject<RWChartEntryRepo.UpdateEvent> updates = PublishSubject.create();
  private final ObservationEntryDao observationEntryDao;
  private final WellnessEntryDao wellnessEntryDao;
  private final SymptomEntryDao symptomEntryDao;

  RoomChartEntryRepo(AppDatabase db) {
    observationEntryDao = db.observationEntryDao();
    wellnessEntryDao = db.wellnessEntryDao();
    symptomEntryDao = db.symptomEntryDao();
  }

  @Override
  public Flowable<RWChartEntryRepo.UpdateEvent> updateEvents() {
    return updates.toFlowable(BackpressureStrategy.BUFFER);
  }

  @Override
  public Single<List<ChartEntry>> getAllEntries() {
    return Single.zip(
        observationEntryDao.getAllEntries(),
        wellnessEntryDao.getAllEntries(),
        symptomEntryDao.getAllEntries(),
        (observationEntries, wellnessEntries, symptomEntries) -> {
          List<ChartEntry> out = new ArrayList<>(observationEntries.size());
          for (ObservationEntry observationEntry : observationEntries.values()) {
            LocalDate date = observationEntry.getDate();
            out.add(new ChartEntry(date, observationEntry, wellnessEntries.get(date), symptomEntries.get(date)));
          }
          return out;
        });
  }

  @Override
  public Flowable<List<ChartEntry>> getLatestN(int n) {
    return Flowable
        .interval(0, 30, TimeUnit.SECONDS)
        .doOnNext(i -> Timber.v("Tick: %d", i))
        .switchMap(i -> DateUtil
            .nowStream()
            .map(now -> DateUtil.daysBetween(now.minusDays(n - 1), now, false)))
        .distinctUntilChanged()
        .doOnNext(days -> Timber.v("New date range: %s", Iterables.toString(days)))
        .switchMap(this::entriesForDates)
        .doOnNext(i -> Timber.v("New data"));
  }

  private Flowable<List<ChartEntry>> entriesForDates(List<LocalDate> dates) {
    return Flowable.merge(Flowable
        .fromIterable(dates)
        .parallel()
        .map(this::getStream)
        .sequential()
        .toList()
        .toFlowable()
        .map(RxUtil::combineLatest));
  }

  @Override
  public Flowable<List<ChartEntry>> getStreamForCycle(Flowable<Cycle> cycleStream) {
    return cycleStream
        .distinctUntilChanged()
        .map(cycle -> Pair.create(
            cycle.startDate,
            Optional.fromNullable(cycle.endDate).or(LocalDate.now())))
        .flatMap(pair -> Flowable.combineLatest(
            observationEntryDao.getIndexedStream(pair.first, pair.second),
            wellnessEntryDao.getIndexedStream(pair.first, pair.second),
            symptomEntryDao.getIndexedStream(pair.first, pair.second),
            (observationStream, wellnessStream, symptomStream) -> {
              List<ChartEntry> out = new ArrayList<>();
              for (LocalDate d : DateUtil.daysBetween(pair.first, pair.second, false)) {
                out.add(new ChartEntry(d, observationStream.get(d), wellnessStream.get(d), symptomStream.get(d)));
              }
              return out;
            }));
  }

  @Override
  public Completable insert(ChartEntry entry) {
    return Completable.mergeArray(
        observationEntryDao.insert(entry.observationEntry),
        wellnessEntryDao.insert(entry.wellnessEntry),
        symptomEntryDao.insert(entry.symptomEntry))
        .doOnComplete(() -> updates.onNext(RWChartEntryRepo.UpdateEvent.forEntry(entry)));
  }

  @Override
  public Completable deleteAll() {
    return Completable.mergeArray(
        observationEntryDao.deleteAll(),
        wellnessEntryDao.deleteAll(),
        symptomEntryDao.deleteAll())
        .doOnSubscribe(s -> Timber.i("Deleting all entries"))
        .doOnComplete(() -> Timber.i("Done deleting all entries"));
  }

  @Override
  public Completable delete(ChartEntry entry) {
    return Completable.mergeArray(
        observationEntryDao.delete(entry.observationEntry),
        wellnessEntryDao.delete(entry.wellnessEntry),
        symptomEntryDao.delete(entry.symptomEntry))
        .doOnComplete(() -> updates.onNext(RWChartEntryRepo.UpdateEvent.forEntry(entry)));
  }

  private Flowable<ChartEntry> getStream(LocalDate entryDate) {
    return Flowable
        .combineLatest(
            Flowable.just(entryDate),
            observationEntryDao.getStream(entryDate),
            wellnessEntryDao.getStream(entryDate),
            symptomEntryDao.getStream(entryDate),
            ChartEntry::new);
  }

  private static Flowable<List<LocalDate>> datesForCycle(Cycle cycle) {
    Flowable<LocalDate> endDate = cycle.endDate != null ? Flowable.just(cycle.endDate) : DateUtil.nowStream();
    return endDate.map(lastDay -> DateUtil.daysBetween(cycle.startDate, lastDay, true));
  }

}

package com.roamingroths.cmcc.data.repos;

import com.google.common.base.Optional;
import com.roamingroths.cmcc.data.db.AppDatabase;
import com.roamingroths.cmcc.data.db.ObservationEntryDao;
import com.roamingroths.cmcc.data.db.SymptomEntryDao;
import com.roamingroths.cmcc.data.db.WellnessEntryDao;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.data.models.ChartEntry;
import com.roamingroths.cmcc.utils.DateUtil;
import com.roamingroths.cmcc.utils.RxUtil;

import org.joda.time.LocalDate;

import java.util.Collection;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import timber.log.Timber;

public class ChartEntryRepo {

  private final ObservationEntryDao observationEntryDao;
  private final WellnessEntryDao wellnessEntryDao;
  private final SymptomEntryDao symptomEntryDao;

  public ChartEntryRepo(AppDatabase db) {
    observationEntryDao = db.observationEntryDao();
    wellnessEntryDao = db.wellnessEntryDao();
    symptomEntryDao = db.symptomEntryDao();
  }

  public Flowable<List<ChartEntry>> getStream(Flowable<Cycle> cycleStream) {
    return Flowable.merge(cycleStream
        .map(ChartEntryRepo::datesForCycle)
        .doOnNext(dates -> Timber.v("Fetching %d entries for cycle", dates.size()))
        .distinctUntilChanged()
        .switchMap(dates -> Flowable
            .fromIterable(dates)
            .map(this::getStream)
            .toList()
            .toFlowable()
            .map(RxUtil::combineLatest)
        ))
        .doOnSubscribe(s -> Timber.v("Fetching entries"));
  }

  public Completable insertAll(Collection<ChartEntry> entries) {
    return Observable
        .fromIterable(entries)
        .flatMapCompletable(this::insert);
  }

  public Completable insert(ChartEntry entry) {
    return Completable.mergeArray(
        observationEntryDao.insert(entry.observationEntry),
        wellnessEntryDao.insert(entry.wellnessEntry),
        symptomEntryDao.insert(entry.symptomEntry));
  }

  public Completable deleteAll() {
    return Completable.mergeArray(
        observationEntryDao.deleteAll(),
        wellnessEntryDao.deleteAll(),
        symptomEntryDao.deleteAll())
        .doOnSubscribe(s -> Timber.i("Deleting all entries"))
        .doOnComplete(() -> Timber.i("Done deleting all entries"));
  }

  public Completable delete(ChartEntry entry) {
    return Completable.mergeArray(
        observationEntryDao.delete(entry.observationEntry),
        wellnessEntryDao.delete(entry.wellnessEntry),
        symptomEntryDao.delete(entry.symptomEntry));
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

  private static List<LocalDate> datesForCycle(Cycle cycle) {
    LocalDate lastDate = Optional.fromNullable(cycle.endDate).or(LocalDate.now());
    return DateUtil.daysBetween(cycle.startDate, lastDate);
  }
}

package com.roamingroths.cmcc.data.repos;

import androidx.core.util.Pair;

import com.google.common.base.Optional;
import com.google.common.collect.ForwardingList;
import com.roamingroths.cmcc.data.db.AppDatabase;
import com.roamingroths.cmcc.data.db.ObservationEntryDao;
import com.roamingroths.cmcc.data.db.SymptomEntryDao;
import com.roamingroths.cmcc.data.db.WellnessEntryDao;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.data.models.ChartEntry;
import com.roamingroths.cmcc.utils.DateUtil;
import com.roamingroths.cmcc.utils.RxUtil;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.flowables.GroupedFlowable;
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

  @Deprecated
  public Flowable<List<ChartEntry>> getStream(Flowable<Cycle> cycleStream) {
    return cycleStream
        .map(ChartEntryRepo::datesForCycle)
        .doOnNext(dates -> Timber.v("Fetching %d entries for cycle", dates.size()))
        .distinctUntilChanged()
        .switchMap(this::entriesForDates)
        .doOnSubscribe(s -> Timber.v("Fetching entries"));
  }

  public Flowable<List<ChartEntry>> getLatestN(int n) {
    return Flowable
        .interval(0, 30, TimeUnit.SECONDS)
        .doOnNext(i -> Timber.v("Tick"))
        .map(i -> DateUtil.daysBetween(LocalDate.now().minusDays(n - 1), LocalDate.now(), false))
        .distinctUntilChanged()
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
    return DateUtil.daysBetween(cycle.startDate, lastDate, true);
  }



  public static class ChartEntryList extends ForwardingList<ChartEntry> {

    private CompositeDisposable mDisposables = new CompositeDisposable();
    private List<ChartEntry> mList = new ArrayList<>();

    @Override
    protected List<ChartEntry> delegate() {
      return mList;
    }

    public Disposable listen(Flowable<GroupedFlowable<LocalDate, ChartEntry>> stream) {
      return stream
          .doOnComplete(() -> mDisposables.clear())
          .subscribe(groupedStream -> {
            mDisposables.add(groupedStream
                .lastElement()
                .subscribe(lastEntry -> mList.remove(lastEntry)));
            mDisposables.add(groupedStream
                .subscribe(latestEntry -> mList.add(latestEntry)));
          });
    }
  }
}

package com.bloomcyclecare.cmcc.data.repos.entry;

import android.util.Range;

import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.db.MeasurementEntryDao;
import com.bloomcyclecare.cmcc.data.db.ObservationEntryDao;
import com.bloomcyclecare.cmcc.data.db.SymptomEntryDao;
import com.bloomcyclecare.cmcc.data.db.WellnessEntryDao;
import com.bloomcyclecare.cmcc.data.models.charting.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.charting.Cycle;
import com.bloomcyclecare.cmcc.data.models.observation.ObservationEntry;
import com.bloomcyclecare.cmcc.data.repos.sticker.RWStickerSelectionRepo;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.bloomcyclecare.cmcc.utils.RxUtil;
import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
  private final MeasurementEntryDao measurementEntryDao;
  private final RWStickerSelectionRepo stickerSelectionRepo;
  private final AtomicBoolean batchUpdate = new AtomicBoolean();

  RoomChartEntryRepo(AppDatabase db, RWStickerSelectionRepo stickerSelectionRepo) {
    observationEntryDao = db.observationEntryDao();
    wellnessEntryDao = db.wellnessEntryDao();
    symptomEntryDao = db.symptomEntryDao();
    measurementEntryDao = db.measurementEntryDao();
    this.stickerSelectionRepo = stickerSelectionRepo;
  }

  @Override
  public boolean beginBatchUpdates() {
    if (!batchUpdate.compareAndSet(false, true)) {
      Timber.w("Already in batch mode!");
      return false;
    }
    return true;
  }

  @Override
  public boolean completeBatchUpdates() {
    if (!batchUpdate.compareAndSet(true, false)) {
      Timber.w("Not in batch mode!");
      return false;
    }
    // TODO: something fancier
    updates.onNext(new UpdateEvent(DateTime.now(), LocalDate.now()));
    return true;
  }

  private void maybeSendUpdate(ChartEntry entry) {
    if (batchUpdate.get()) {
      Timber.d("Skipping update while in batch mode");
      return;
    }
    updates.onNext(UpdateEvent.forEntry(entry));
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
        measurementEntryDao.getAllEntries(),
        stickerSelectionRepo.getSelections().firstOrError(),
        (observationEntries, wellnessEntries, symptomEntries, measurementEntries, stickerSelections) -> {
          List<ChartEntry> out = new ArrayList<>(observationEntries.size());
          for (ObservationEntry observationEntry : observationEntries.values()) {
            LocalDate date = observationEntry.getDate();
            out.add(new ChartEntry(date, observationEntry, wellnessEntries.get(date), symptomEntries.get(date), measurementEntries.get(date), stickerSelections.get(date)));
          }
          return out;
        });
  }

  @Override
  public Flowable<List<ChartEntry>> getLatestN(int n) {
    return Flowable
        .interval(0, 30, TimeUnit.SECONDS)
        .switchMap(i -> DateUtil
            .nowStream()
            .map(now -> DateUtil.daysBetween(now.minusDays(n - 1), now, false)))
        .distinctUntilChanged()
        .switchMap(this::entriesForDates);
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
            measurementEntryDao.getIndexedStream(pair.first, pair.second),
            stickerSelectionRepo.getSelections(Range.create(pair.first, pair.second)),
            (observationStream, wellnessStream, symptomStream, measurementStream, stickerSelections) -> {
              List<ChartEntry> out = new ArrayList<>();
              for (LocalDate d : DateUtil.daysBetween(pair.first, pair.second, false)) {
                out.add(new ChartEntry(d, observationStream.get(d), wellnessStream.get(d), symptomStream.get(d), measurementStream.get(d), stickerSelections.get(d)));
              }
              return out;
            }));
  }

  @Override
  public Completable insert(ChartEntry entry) {
    return Completable.mergeArray(
        observationEntryDao.insertNullable(entry.observationEntry),
        wellnessEntryDao.insertNullable(entry.wellnessEntry),
        stickerSelectionRepo.recordSelection(entry.stickerSelection, entry.entryDate),
        measurementEntryDao.insertNullable(entry.measurementEntry),
        symptomEntryDao.insertNullable(entry.symptomEntry))
        .doOnComplete(() -> maybeSendUpdate(entry));
  }

  @Override
  public Completable deleteAll() {
    return Completable.mergeArray(
        observationEntryDao.deleteAll(),
        wellnessEntryDao.deleteAll(),
        stickerSelectionRepo.deleteAll(),
        measurementEntryDao.deleteAll(),
        symptomEntryDao.deleteAll())
        .doOnSubscribe(s -> Timber.i("Deleting all entries"))
        .doOnComplete(() -> Timber.i("Done deleting all entries"));
  }

  @Override
  public Completable delete(ChartEntry entry) {
    return Completable.mergeArray(
        observationEntryDao.delete(entry.observationEntry),
        wellnessEntryDao.delete(entry.wellnessEntry),
        stickerSelectionRepo.delete(entry.entryDate),
        measurementEntryDao.delete(entry.measurementEntry),
        symptomEntryDao.delete(entry.symptomEntry))
        .doOnComplete(() -> maybeSendUpdate(entry));
  }

  private Flowable<ChartEntry> getStream(LocalDate entryDate) {
    return Flowable
        .combineLatest(
            Flowable.just(entryDate),
            observationEntryDao.getStream(entryDate),
            wellnessEntryDao.getStream(entryDate),
            symptomEntryDao.getStream(entryDate),
            measurementEntryDao.getStream(entryDate),
            stickerSelectionRepo.getSelectionStream(entryDate),
            (d, o, w, s, m, ss) -> new ChartEntry(d, o, w, s, m, ss.orElse(null)));
  }
}

package com.bloomcyclecare.cmcc.data.repos.entry;

import android.util.Range;

import androidx.core.util.Pair;

import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.db.BreastfeedingEntryDao;
import com.bloomcyclecare.cmcc.data.db.MedicationDao;
import com.bloomcyclecare.cmcc.data.db.WellbeingEntryDao;
import com.bloomcyclecare.cmcc.data.db.MeasurementEntryDao;
import com.bloomcyclecare.cmcc.data.db.ObservationEntryDao;
import com.bloomcyclecare.cmcc.data.models.charting.ChartEntry;
import com.bloomcyclecare.cmcc.data.models.charting.Cycle;
import com.bloomcyclecare.cmcc.data.models.observation.ObservationEntry;
import com.bloomcyclecare.cmcc.data.models.wellbeing.WellbeingEntryWithRelations;
import com.bloomcyclecare.cmcc.data.repos.sticker.RWStickerSelectionRepo;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.bloomcyclecare.cmcc.utils.RxUtil;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;

class RoomChartEntryRepo implements RWChartEntryRepo {

  private final PublishSubject<RWChartEntryRepo.UpdateEvent> updates = PublishSubject.create();
  private final ObservationEntryDao observationEntryDao;
  private final MeasurementEntryDao measurementEntryDao;
  private final BreastfeedingEntryDao breastfeedingEntryDao;
  private final WellbeingEntryDao wellbeingEntryDao;
  private final MedicationDao medicationDao;
  private final RWStickerSelectionRepo stickerSelectionRepo;
  private final AtomicBoolean batchUpdate = new AtomicBoolean();

  RoomChartEntryRepo(AppDatabase db, RWStickerSelectionRepo stickerSelectionRepo) {
    observationEntryDao = db.observationEntryDao();
    measurementEntryDao = db.measurementEntryDao();
    breastfeedingEntryDao = db.breastfeedingEntryDao();
    wellbeingEntryDao = db.lifestyleEntryDao();
    medicationDao = db.medicationDao();

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
    return observationEntryDao.getAllEntries()
        .flatMap(observationEntries -> Single.zip(
            medicationDao.getIndexedRefStream(observationEntries.firstKey(), observationEntries.lastKey()).firstOrError(),
            measurementEntryDao.getAllEntries(),
            breastfeedingEntryDao.getAllEntries(),
            wellbeingEntryDao.getAllEntries(),
            stickerSelectionRepo.getSelections().firstOrError(),
            (medicationRefs, measurementEntries, breastfeedingEntries, lifestyleEntries, stickerSelections) -> {
              List<ChartEntry> out = new ArrayList<>(observationEntries.size());
              for (ObservationEntry observationEntry : observationEntries.values()) {
                LocalDate date = observationEntry.getDate();
                out.add(new ChartEntry(date, observationEntry, measurementEntries.get(date), breastfeedingEntries.get(date), WellbeingEntryWithRelations.create(lifestyleEntries.get(date), medicationRefs.get(date)), stickerSelections.get(date)));
              }
              return out;
            }));
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
            Optional.ofNullable(cycle.endDate).orElse(LocalDate.now())))
        .flatMap(pair -> getAllBetween(pair.first, pair.second));
  }

  @Override
  public Flowable<List<ChartEntry>> getAllBetween(LocalDate start, LocalDate endInclusive) {
    return Flowable.combineLatest(
        observationEntryDao.getIndexedStream(start, endInclusive)
            .doOnNext(n -> Timber.v("Got new observation stream for cycle starting %s", start)),
        measurementEntryDao.getIndexedStream(start, endInclusive)
            .doOnNext(n -> Timber.v("Got new measurement stream for cycle starting %s", start)),
        breastfeedingEntryDao.getIndexedStream(start, endInclusive)
            .doOnNext(n -> Timber.v("Got new breastfeeding stream for cycle starting %s", start)),
        wellbeingEntryDao.getIndexedStream(start, endInclusive)
            .doOnNext(n -> Timber.v("Got new lifestyle stream for cycle starting %s", start)),
        medicationDao.getIndexedRefStream(start, endInclusive)
            .doOnNext(n -> Timber.v("Got new medication ref stream for cycle starting %s", start)),
        stickerSelectionRepo.getSelections(Range.create(start, endInclusive))
            .doOnNext(n -> Timber.v("Got new selections for cycle starting %s", start)),
        (observationStream, measurementStream, breastfeedingStream, wellbeingStream, medicationRefStream, stickerSelections) -> {
          List<ChartEntry> out = new ArrayList<>();
          for (LocalDate d : DateUtil.daysBetween(start, endInclusive, false)) {
            out.add(new ChartEntry(d, observationStream.get(d), measurementStream.get(d), breastfeedingStream.get(d), WellbeingEntryWithRelations.create(wellbeingStream.get(d), medicationRefStream.get(d)), stickerSelections.get(d)));
          }
          return out;
        });
  }

  @Override
  public Completable insert(ChartEntry entry) {
    return Completable.mergeArray(
        observationEntryDao.insertNullable(entry.observationEntry),
        stickerSelectionRepo.recordSelection(entry.stickerSelection, entry.entryDate),
        measurementEntryDao.insertNullable(entry.measurementEntry),
        breastfeedingEntryDao.insertNullable(entry.breastfeedingEntry),
        medicationDao.deleteRefs(entry.entryDate),
        medicationDao.insert(entry.wellbeingEntry.medicationRefs),
        wellbeingEntryDao.insertNullable(entry.wellbeingEntry.wellbeingEntry))
        .doOnComplete(() -> maybeSendUpdate(entry));
  }

  @Override
  public Completable deleteAll() {
    return Completable.mergeArray(
        observationEntryDao.deleteAll(),
        stickerSelectionRepo.deleteAll(),
        measurementEntryDao.deleteAll(),
        breastfeedingEntryDao.deleteAll(),
        medicationDao.deleteAllMedications(),
        medicationDao.deleteAllRefs(),
        wellbeingEntryDao.deleteAll())
        .doOnSubscribe(s -> Timber.i("Deleting all entries"))
        .doOnComplete(() -> Timber.i("Done deleting all entries"));
  }

  @Override
  public Completable delete(ChartEntry entry) {
    return Completable.mergeArray(
        observationEntryDao.delete(entry.observationEntry),
        stickerSelectionRepo.delete(entry.entryDate),
        measurementEntryDao.delete(entry.measurementEntry),
        breastfeedingEntryDao.delete(entry.breastfeedingEntry),
        medicationDao.delete(entry.wellbeingEntry.medicationRefs),
        wellbeingEntryDao.delete(entry.wellbeingEntry.wellbeingEntry))
        .doOnComplete(() -> maybeSendUpdate(entry));
  }

  private Flowable<ChartEntry> getStream(LocalDate entryDate) {
    return Flowable
        .combineLatest(
            Flowable.just(entryDate),
            observationEntryDao.getStream(entryDate),
            measurementEntryDao.getStream(entryDate),
            breastfeedingEntryDao.getStream(entryDate),
            wellbeingEntryDao.getStream(entryDate),
            medicationDao.getAllRefs(entryDate),
            stickerSelectionRepo.getSelectionStream(entryDate),
            (d, o, m, b, w, mrfs, ss) -> new ChartEntry(d, o, m, b, WellbeingEntryWithRelations.create(w, mrfs), ss.orElse(null)));
  }
}

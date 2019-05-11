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

import java.util.List;

import io.reactivex.Flowable;
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
        .distinctUntilChanged()
        .switchMap(dates -> Flowable
            .fromIterable(dates)
            .map(this::getStream)
            .toList()
            .toFlowable()
            .map(RxUtil::combineLatest)
        ));
  }

  private Flowable<ChartEntry> getStream(LocalDate entryDate) {
    return Flowable
        .combineLatest(
            Flowable.just(entryDate),
            observationEntryDao.getStream(entryDate),
            wellnessEntryDao.getStream(entryDate),
            symptomEntryDao.getStream(entryDate),
            ChartEntry::new)
        .doOnSubscribe(s -> Timber.d("New ChartEntry stream for %s", entryDate))
        .doOnNext(e -> Timber.d("New data on ChartEntry stream for %s", entryDate));
  }

  private static List<LocalDate> datesForCycle(Cycle cycle) {
    LocalDate lastDate = Optional.fromNullable(cycle.endDate).or(LocalDate.now());
    return DateUtil.daysBetween(cycle.startDate, lastDate);
  }
}

package com.bloomcyclecare.cmcc.data.db;

import androidx.arch.core.util.Function;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.RawQuery;
import androidx.room.Update;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.bloomcyclecare.cmcc.data.models.Entry;
import com.bloomcyclecare.cmcc.data.models.breastfeeding.BreastfeedingEntry;
import com.bloomcyclecare.cmcc.data.models.wellbeing.WellbeingEntry;
import com.bloomcyclecare.cmcc.data.models.measurement.MeasurementEntry;
import com.bloomcyclecare.cmcc.data.models.medication.MedicationEntry;
import com.bloomcyclecare.cmcc.data.models.observation.ObservationEntry;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelectionEntry;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nullable;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

// NOTE: new children of this class need to be added to the decorators below!!!!!

@Dao
public abstract class BaseEntryDao<E extends Entry> {

  private final String mTableName;
  private final Function<LocalDate, E> mEmptyEntryFn;

  BaseEntryDao(Class<E> clazz, Function<LocalDate, E> emptyEntryFn) {
    mTableName = clazz.getSimpleName();
    mEmptyEntryFn = emptyEntryFn;
  }

  public Maybe<E> get(LocalDate entryDate) {
    return doMaybeT(new SimpleSQLiteQuery(String.format(
        "SELECT * FROM %s WHERE entryDate = '%s'",
        mTableName,
        DateUtil.toWireStr(entryDate))));
  }

  public Single<TreeMap<LocalDate, E>> getAllEntries() {
    return doFlowableList(new SimpleSQLiteQuery(String.format(
        "SELECT * FROM %s ORDER BY entryDate ASC ",
        mTableName)))
        .firstOrError()
        .map(entries -> {
          TreeMap<LocalDate, E> out = new TreeMap<>();
          for (E e : entries) {
            out.put(e.getDate(), e);
          }
          return out;
        });
  }

  public Flowable<E> getStream(LocalDate entryDate) {
    SimpleSQLiteQuery query = new SimpleSQLiteQuery(String.format(
        "SELECT * FROM %s WHERE entryDate = '%s'",
        mTableName,
        DateUtil.toWireStr(entryDate)));
    return doMaybeT(query)
        .map(Optional::of)
        .toSingle(Optional.absent())
        .flatMapPublisher(initialEntry -> doFlowableT(query)
            .startWith(initialEntry.or(mEmptyEntryFn.apply(entryDate))))
        .distinctUntilChanged();
  }

  public Flowable<Optional<E>> getOptionalStream(LocalDate entryDate) {
    SimpleSQLiteQuery query = new SimpleSQLiteQuery(String.format(
        "SELECT * FROM %s WHERE entryDate = '%s'",
        mTableName,
        DateUtil.toWireStr(entryDate)));
    return doMaybeT(query)
        .map(Optional::of)
        .toSingle(Optional.absent())
        .flatMapPublisher(initialEntry -> doFlowableT(query)
            .map(Optional::of)
            .startWith(initialEntry))
        .distinctUntilChanged();
  }

  public Flowable<Map<LocalDate, E>> getIndexedStream(LocalDate firstDate, LocalDate lastDate) {
    return doFlowableList(new SimpleSQLiteQuery(String.format(
        "SELECT * FROM %s WHERE entryDate >= '%s' AND entryDate <= '%s' ORDER BY entryDate",
        mTableName,
        DateUtil.toWireStr(firstDate),
        DateUtil.toWireStr(lastDate))))
        .distinctUntilChanged((l1, l2) -> {
          Set<E> s1 = new HashSet<>(l1);
          Set<E> s2 = new HashSet<>(l2);
          return Sets.difference(s1, s2).isEmpty() && Sets.difference(s2, s1).isEmpty();
        })
        .map(entries -> {
          List<LocalDate> daysMissingData = DateUtil.daysBetween(firstDate, lastDate, false);
          Map<LocalDate, E> out = new HashMap<>(daysMissingData.size());
          for (E entry : entries) {
            out.put(entry.getDate(), entry);
            daysMissingData.remove(entry.getDate());
          }
          for (LocalDate d : daysMissingData) {
            if (!out.containsKey(d)) {
              out.put(d, mEmptyEntryFn.apply(d));
            }
          }
          return out;
        })
        ;
  }

  public Flowable<List<E>> getStream() {
    return doFlowableList(new SimpleSQLiteQuery(String.format(
        "SELECT * FROM %s",
        mTableName)))
        .distinctUntilChanged();
  }

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract Completable insertInternal(E entry);

  public final Completable insert(E entry) {
    DateTime now = DateTime.now();
    if (entry.mTimeCreated == null) {
      entry.mTimeCreated = now;
    }
    entry.mTimeUpdated = now;
    entry.mTimesUpdated = ++entry.mTimesUpdated;
    return insertInternal(entry);
  }

  public Completable insertNullable(@Nullable E entry) {
    if (entry == null) {
      return Completable.complete();
    }
    return insert(entry);
  }

  @Delete
  public abstract Completable delete(E entry);

  @Delete
  public abstract Completable delete(List<E> entries);

  public Completable deleteAll() {
    return getStream()
        .firstOrError()
        .flatMapCompletable(this::delete);
  }

  @Update
  abstract Completable updateInternal(E entry);

  public final Completable update(E entry) {
    if (entry.mTimeCreated == null) {
      return Completable.error(new IllegalArgumentException("timeCreated == null"));
    }
    if (entry.mTimesUpdated <= 0) {
      return Completable.error(new IllegalArgumentException("timesUpdated <= 0"));
    }
    entry.mTimeUpdated = DateTime.now();
    entry.mTimesUpdated = ++entry.mTimesUpdated;
    return updateInternal(entry);
  }

  @RawQuery(observedEntities = {
      ObservationEntry.class,
      MeasurementEntry.class,
      BreastfeedingEntry.class,
      StickerSelectionEntry.class,
      MedicationEntry.class,
      WellbeingEntry.class,
  })
  protected abstract Maybe<E> doMaybeT(SupportSQLiteQuery query);

  @RawQuery(observedEntities = {
      ObservationEntry.class,
      MeasurementEntry.class,
      BreastfeedingEntry.class,
      StickerSelectionEntry.class,
      MedicationEntry.class,
      WellbeingEntry.class,
  })
  protected abstract Flowable<E> doFlowableT(SupportSQLiteQuery query);

  @RawQuery(observedEntities = {
      ObservationEntry.class,
      MeasurementEntry.class,
      BreastfeedingEntry.class,
      StickerSelectionEntry.class,
      MedicationEntry.class,
      WellbeingEntry.class,
  })
  protected abstract Flowable<List<E>> doFlowableList(SupportSQLiteQuery query);
}

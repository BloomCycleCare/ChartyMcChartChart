package com.bloomcyclecare.cmcc.data.db;

import com.bloomcyclecare.cmcc.data.entities.Entry;
import com.bloomcyclecare.cmcc.data.entities.ObservationEntry;
import com.bloomcyclecare.cmcc.data.entities.SymptomEntry;
import com.bloomcyclecare.cmcc.data.entities.WellnessEntry;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.google.common.base.Optional;

import org.joda.time.LocalDate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import androidx.arch.core.util.Function;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.RawQuery;
import androidx.room.Update;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

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
        "SELECT * FROM %s WHERE entryDate = \"%s\"",
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
        "SELECT * FROM %s WHERE entryDate = \"%s\"",
        mTableName,
        DateUtil.toWireStr(entryDate)));
    return doMaybeT(query)
        .map(Optional::of)
        .toSingle(Optional.absent())
        .flatMapPublisher(initialEntry -> doFlowableT(query)
            .startWith(initialEntry.or(mEmptyEntryFn.apply(entryDate))))
        .distinctUntilChanged();
  }

  public Flowable<Map<LocalDate, E>> getIndexedStream(LocalDate firstDate, LocalDate lastDate) {
    return doFlowableList(new SimpleSQLiteQuery(String.format(
        "SELECT * FROM %s WHERE entryDate >= \"%s\" AND entryDate <= \"%s\" ORDER BY entryDate",
        mTableName,
        DateUtil.toWireStr(firstDate),
        DateUtil.toWireStr(lastDate))))
        .distinctUntilChanged()
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
  public abstract Completable insert(E entry);

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
  public abstract Completable update(E entry);

  @RawQuery(observedEntities = {
      ObservationEntry.class,
      WellnessEntry.class,
      SymptomEntry.class,
  })
  protected abstract Maybe<E> doMaybeT(SupportSQLiteQuery query);

  @RawQuery(observedEntities = {
      ObservationEntry.class,
      WellnessEntry.class,
      SymptomEntry.class,
  })
  protected abstract Flowable<E> doFlowableT(SupportSQLiteQuery query);

  @RawQuery(observedEntities = {
      ObservationEntry.class,
      WellnessEntry.class,
      SymptomEntry.class,
  })
  protected abstract Flowable<List<E>> doFlowableList(SupportSQLiteQuery query);
}

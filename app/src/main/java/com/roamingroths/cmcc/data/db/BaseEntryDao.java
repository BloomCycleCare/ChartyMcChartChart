package com.roamingroths.cmcc.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Update;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.roamingroths.cmcc.data.entities.Entry;
import com.roamingroths.cmcc.data.entities.ObservationEntry;
import com.roamingroths.cmcc.data.entities.SymptomEntry;
import com.roamingroths.cmcc.data.entities.WellnessEntry;
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

@Dao
public abstract class BaseEntryDao<E extends Entry> {

  private String mTableName;

  BaseEntryDao(Class<E> clazz) {
    mTableName = clazz.getSimpleName();
  }

  public Flowable<E> getStream(LocalDate entryDate) {
    return doFind(new SimpleSQLiteQuery(String.format(
        "SELECT * FROM %s WHERE entryDate = %s",
        mTableName,
        DateUtil.toWireStr(entryDate))))
        .distinctUntilChanged();
  }

  public Flowable<List<E>> getStream(LocalDate firstDate, LocalDate lastDate) {
    return doFindBetween(new SimpleSQLiteQuery(String.format(
        "SELECT * FROM %s WHERE entryDate >= %s AND entryDate <= %s ORDER BY entryDate",
        mTableName,
        DateUtil.toWireStr(firstDate),
        DateUtil.toWireStr(lastDate))))
        .distinctUntilChanged();
  }

  @Insert
  public abstract Completable insert(E entry);

  @Delete
  public abstract Completable delete(E entry);

  @Update
  public abstract Completable update(E entry);

  @RawQuery(observedEntities = {
      ObservationEntry.class,
      WellnessEntry.class,
      SymptomEntry.class,
  })
  protected abstract Flowable<E> doFind(SupportSQLiteQuery query);

  @RawQuery(observedEntities = {
      ObservationEntry.class,
      WellnessEntry.class,
      SymptomEntry.class,
  })
  protected abstract Flowable<List<E>> doFindBetween(SupportSQLiteQuery query);
}

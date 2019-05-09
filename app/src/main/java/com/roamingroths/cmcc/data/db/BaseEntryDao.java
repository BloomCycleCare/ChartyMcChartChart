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
import com.roamingroths.cmcc.utils.DateUtil;

import org.joda.time.LocalDate;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;

@Dao
public abstract class BaseEntryDao<E extends Entry> {

  private String mTableName;

  BaseEntryDao(Class<E> clazz) {
    mTableName = clazz.getSimpleName();
  }

  public Maybe<E> get(LocalDate entryDate) {
    return doFind(new SimpleSQLiteQuery(String.format(
        "SELECT * FROM %s WHERE entryDate = %s",
        mTableName,
        DateUtil.toWireStr(entryDate))));
  }

  public Single<List<E>> getAll(LocalDate firstDate, LocalDate lastDate) {
    return doFindBetween(new SimpleSQLiteQuery(String.format(
        "SELECT * FROM %s WHERE entryDate >= %s AND entryDate <= %s",
        mTableName,
        DateUtil.toWireStr(firstDate),
        DateUtil.toWireStr(lastDate))));
  }

  @Insert
  public abstract Completable insert(E entry);

  @Delete
  public abstract Completable delete(E entry);

  @Update
  public abstract Completable update(E entry);

  @RawQuery
  protected abstract Maybe<E> doFind(SupportSQLiteQuery query);

  @RawQuery
  protected abstract Single<List<E>> doFindBetween(SupportSQLiteQuery query);
}

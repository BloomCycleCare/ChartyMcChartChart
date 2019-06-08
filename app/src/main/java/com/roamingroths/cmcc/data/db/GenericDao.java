package com.roamingroths.cmcc.data.db;

import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;

import java.util.Collection;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;

public interface GenericDao <T> {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(Collection<T> values);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(T value);

  @Delete
  Completable delete(T value);

  Single<List<T>> getAll();

  Flowable<List<T>> getStream();
}

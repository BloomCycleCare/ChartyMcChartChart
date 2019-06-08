package com.roamingroths.cmcc.data.db;

import androidx.room.Insert;
import androidx.room.OnConflictStrategy;

import java.util.Collection;

import io.reactivex.Completable;

public interface GenericDao <T> {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(Collection<T> values);
}

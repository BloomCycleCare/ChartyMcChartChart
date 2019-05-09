package com.roamingroths.cmcc.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;

import com.roamingroths.cmcc.data.entities.Cycle;

import io.reactivex.Completable;

@Dao
public interface CycleDao {

  @Delete
  Completable delete(Cycle cycle);

  @Insert
  Completable insert(Cycle cycle);

  @Insert
  Completable insert(Iterable<Cycle> cycles);

  @Insert
  Completable insert(Cycle... cycles);
}

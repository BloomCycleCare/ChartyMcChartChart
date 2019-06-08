package com.roamingroths.cmcc.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;

import com.roamingroths.cmcc.data.entities.Instructions;

import org.joda.time.LocalDate;

import java.util.Collection;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;

@Dao
public interface InstructionDao extends GenericDao<Instructions> {

  @Query("SELECT * FROM Instructions")
  Flowable<List<Instructions>> getAll();

  @TypeConverters(Converters.class)
  @Query("SELECT * FROM Instructions WHERE :endDate IS NULL")
  Maybe<Instructions> getCurrent(LocalDate endDate);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(Instructions instructions);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  Completable insert(Collection<Instructions> instructions);
}

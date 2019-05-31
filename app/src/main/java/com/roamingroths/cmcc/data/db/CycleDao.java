package com.roamingroths.cmcc.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;
import androidx.room.Update;

import com.roamingroths.cmcc.data.entities.Cycle;

import org.joda.time.LocalDate;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;

@Dao
public abstract class CycleDao {

  @Query("SELECT * FROM Cycle ORDER BY startDate DESC")
  public abstract Flowable<List<Cycle>> getStream();

  @Query("SELECT * FROM Cycle WHERE endDate IS NULL")
  public abstract Maybe<Cycle> getCurrentCycle();

  @TypeConverters(Converters.class)
  @Query("SELECT * FROM Cycle WHERE endDate=:endDate")
  public abstract Maybe<Cycle> getCycleWithEndDate(LocalDate endDate);

  @Delete
  public abstract Completable delete(Cycle cycle);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public abstract Completable insert(Cycle cycle);

  @Update
  public abstract Completable update(Cycle cycle);
}

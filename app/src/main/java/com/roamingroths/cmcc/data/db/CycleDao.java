package com.roamingroths.cmcc.data.db;

import com.roamingroths.cmcc.data.entities.Cycle;

import org.joda.time.LocalDate;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.TypeConverters;
import androidx.room.Update;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

@Dao
public abstract class CycleDao {

  @Query("SELECT * FROM Cycle ORDER BY startDate DESC")
  public abstract Flowable<List<Cycle>> getStream();

  @Query("SELECT * FROM Cycle WHERE endDate IS NULL")
  public abstract Maybe<Cycle> getCurrentCycle();

  @TypeConverters(Converters.class)
  @Query("SELECT * FROM Cycle WHERE endDate=:endDate")
  public abstract Maybe<Cycle> getCycleWithEndDate(LocalDate endDate);

  @TypeConverters(Converters.class)
  @Query("SELECT * FROM Cycle WHERE startDate <= :date AND (endDate IS NULL OR endDate >= :date)")
  public abstract Maybe<Cycle> getCycleForDate(LocalDate date);

  @Delete
  public abstract Completable delete(Cycle cycle);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public abstract Completable insert(Cycle cycle);

  @Update
  public abstract Completable update(Cycle cycle);

  @Transaction
  public Cycle startNewCycle(Cycle currentCycle, LocalDate startDate) {
    Cycle newCycle = new Cycle("fooo", startDate, null);
    Cycle copyOfCurrent = new Cycle(currentCycle);
    copyOfCurrent.endDate = startDate.minusDays(1);
    return Completable
        .concatArray(insert(newCycle), update(copyOfCurrent))
        .andThen(Single.just(newCycle)).blockingGet();
  }
}

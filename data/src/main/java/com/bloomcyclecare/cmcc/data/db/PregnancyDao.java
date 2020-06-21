package com.bloomcyclecare.cmcc.data.db;

import com.bloomcyclecare.cmcc.data.models.pregnancy.Pregnancy;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

@Dao
public abstract class PregnancyDao {

  @Query("SELECT * FROM Pregnancy WHERE id = :id")
  public abstract Maybe<Pregnancy> getById(Long id);

  @Query("SELECT * FROM Pregnancy")
  public abstract Flowable<List<Pregnancy>> getAll();

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public abstract Single<Long> insert(Pregnancy pregnancy);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public abstract Completable insert(List<Pregnancy> pregnancies);

  @Delete
  public abstract Completable delete(Pregnancy pregnancy);

  @Update
  public abstract Completable update(Pregnancy pregnancy);
}

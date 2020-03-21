package com.bloomcyclecare.cmcc.data.db;

import com.bloomcyclecare.cmcc.data.entities.Pregnancy;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;

@Dao
public abstract class PregnancyDao {

  @Query("SELECT * FROM Pregnancy WHERE id = :id")
  public abstract Maybe<Pregnancy> getById(Long id);

  @Query("SELECT * FROM Pregnancy")
  public abstract Single<List<Pregnancy>> getAll();

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public abstract Single<Long> insert(Pregnancy pregnancy);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public abstract Completable insert(List<Pregnancy> pregnancies);

  @Delete
  public abstract Completable delete(Pregnancy pregnancy);
}

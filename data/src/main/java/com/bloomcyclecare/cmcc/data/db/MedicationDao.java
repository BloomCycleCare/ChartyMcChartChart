package com.bloomcyclecare.cmcc.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.bloomcyclecare.cmcc.data.models.medication.Medication;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;

@Dao
public abstract class MedicationDao {

  @Query("SELECT * FROM Medication")
  public abstract Flowable<List<Medication>> get();

  @Delete
  public abstract Completable delete(Medication medication);

  @Insert
  public abstract Single<Long> insert(Medication medication);

  @Update
  public abstract Completable update(Medication medication);
}

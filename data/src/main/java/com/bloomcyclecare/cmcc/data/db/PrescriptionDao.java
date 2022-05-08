package com.bloomcyclecare.cmcc.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.bloomcyclecare.cmcc.data.models.medication.Medication;
import com.bloomcyclecare.cmcc.data.models.medication.Prescription;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;

@Dao
public abstract class PrescriptionDao {

  @Query("SELECT * FROM Prescription WHERE medicationId=:medicationId")
  public abstract Flowable<List<Prescription>> getAll(int medicationId);

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public abstract Single<Long> save(Prescription prescription);

  @Delete
  public abstract Completable delete(Prescription prescription);
}

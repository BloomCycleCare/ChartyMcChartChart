package com.bloomcyclecare.cmcc.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;

import com.bloomcyclecare.cmcc.data.models.medication.Medication;

import io.reactivex.Completable;
import io.reactivex.Single;

@Dao
public abstract class MedicationDao {

  @Insert
  public abstract Single<Medication> insert(Medication medication);

  @Update
  public abstract Completable update(Medication medication);
}

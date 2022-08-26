package com.bloomcyclecare.cmcc.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Transaction;
import androidx.room.TypeConverters;
import androidx.room.Update;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.bloomcyclecare.cmcc.data.models.medication.Medication;
import com.bloomcyclecare.cmcc.data.models.medication.MedicationRef;
import com.bloomcyclecare.cmcc.data.models.medication.MedicationWithRelations;
import com.bloomcyclecare.cmcc.data.models.observation.ObservationEntry;
import com.bloomcyclecare.cmcc.utils.DateUtil;
import com.google.common.collect.Sets;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;

@Dao
public abstract class MedicationDao {

  @Query("SELECT * FROM Medication")
  public abstract Flowable<List<Medication>> get();

  @Transaction
  @Query("SELECT * FROM Medication")
  public abstract Flowable<List<MedicationWithRelations>> getDecorated();

  @Delete
  public abstract Completable delete(Medication medication);

  @Insert
  public abstract Single<Long> insert(Medication medication);

  @Update
  public abstract Completable update(Medication medication);

  @TypeConverters(Converters.class)
  @Query("SELECT * FROM MedicationRef WHERE entryDate=:entryDate")
  public abstract Flowable<List<MedicationRef>> getAllRefs(LocalDate entryDate);

  @Query("SELECT * FROM MedicationRef WHERE entryDate >= :firstDate AND entryDate <= :lastDate ORDER BY entryDate")
  public abstract Flowable<List<MedicationRef>> getAllBetween(LocalDate firstDate, LocalDate lastDate);

  public Flowable<Map<LocalDate, List<MedicationRef>>> getIndexedRefStream(LocalDate firstDate, LocalDate lastDate) {
    return getAllBetween(firstDate, lastDate)
        .map(refs -> {
          Map<LocalDate, List<MedicationRef>> out = new HashMap<>();
          DateUtil.daysBetween(firstDate, lastDate, false).forEach(date -> out.put(date, new ArrayList<>()));
          for (MedicationRef ref : refs) {
            out.get(ref.entryDate).add(ref);
          }
          return out;
        });
  }

  public Completable deleteAllMedications() {
    return get().firstOrError().flatMapCompletable(this::deleteMedications);
  }

  @Delete
  public abstract Completable deleteMedications(List<Medication> medications);

  @Query("DELETE FROM MedicationRef WHERE entryDate=:entryDate")
  public abstract Completable deleteRefs(LocalDate entryDate);

  @Query("DELETE FROM MedicationRef")
  public abstract Completable deleteAllRefs();

  @Delete
  public abstract Completable delete(List<MedicationRef> refs);

  @Insert
  public abstract Completable insert(List<MedicationRef> refs);
}

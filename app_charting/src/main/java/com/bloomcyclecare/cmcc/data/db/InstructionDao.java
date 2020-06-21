package com.bloomcyclecare.cmcc.data.db;

import com.bloomcyclecare.cmcc.data.models.instructions.Instructions;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Query;
import io.reactivex.Flowable;
import io.reactivex.Single;

@Dao
public abstract class InstructionDao implements GenericDao<Instructions> {

  @Query("SELECT * FROM Instructions")
  public abstract Single<List<Instructions>> getAll();

  @Query("SELECT * FROM Instructions")
  public abstract Flowable<List<Instructions>> getStream();
}

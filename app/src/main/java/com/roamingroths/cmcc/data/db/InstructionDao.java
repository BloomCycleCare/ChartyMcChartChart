package com.roamingroths.cmcc.data.db;

import androidx.room.Dao;
import androidx.room.Query;

import com.roamingroths.cmcc.data.entities.Instructions;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;

@Dao
public abstract class InstructionDao implements GenericDao<Instructions> {

  @Query("SELECT * FROM Instructions")
  public abstract Single<List<Instructions>> getAll();

  @Query("SELECT * FROM Instructions")
  public abstract Flowable<List<Instructions>> getStream();
}

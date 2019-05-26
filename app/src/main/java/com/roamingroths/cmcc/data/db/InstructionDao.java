package com.roamingroths.cmcc.data.db;

import androidx.room.Dao;
import androidx.room.Query;

import com.roamingroths.cmcc.data.entities.Instructions;

import java.util.List;

import io.reactivex.Flowable;

@Dao
public interface InstructionDao {

  @Query("SELECT * FROM Instructions")
  Flowable<List<Instructions>> getAll();
}

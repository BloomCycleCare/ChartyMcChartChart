package com.bloomcyclecare.cmcc.data.repos.instructions;

import com.bloomcyclecare.cmcc.data.entities.Instructions;

import org.joda.time.LocalDate;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;

public interface ROInstructionsRepo {

  Flowable<Instructions> get(LocalDate startDate);

  Flowable<List<Instructions>> getAll();

  Single<Boolean> hasAnyAfter(LocalDate date);
}

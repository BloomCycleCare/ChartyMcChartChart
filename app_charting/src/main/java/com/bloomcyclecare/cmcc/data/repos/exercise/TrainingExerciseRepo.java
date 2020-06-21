package com.bloomcyclecare.cmcc.data.repos.exercise;

import com.bloomcyclecare.cmcc.models.training.Exercise;

import java.util.List;

import io.reactivex.Single;

public class TrainingExerciseRepo implements RWExerciseRepo {

  @Override
  public Single<List<Exercise>> getAll() {
    return Single.just(Exercise.COLLECTION);
  }
}

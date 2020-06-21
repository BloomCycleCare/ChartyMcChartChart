package com.bloomcyclecare.cmcc.data.repos.exercise;

import com.bloomcyclecare.cmcc.models.training.Exercise;

import java.util.List;

import io.reactivex.Single;

public interface ROExerciseRepo {

  Single<List<Exercise>> getAll();
}

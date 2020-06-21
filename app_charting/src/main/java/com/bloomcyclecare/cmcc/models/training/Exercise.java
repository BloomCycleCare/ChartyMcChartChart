package com.bloomcyclecare.cmcc.models.training;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;

@AutoValue
public abstract class Exercise {

  public static final ImmutableList<Exercise> COLLECTION = ImmutableList.of(
      Exercise.create(ID.CYCLE_REVIEW_REGULAR_CYCLES, TrainingCycles.REGULAR_CYCLES),
      Exercise.create(ID.CYCLE_REVIEW_LONG_CYCLES, TrainingCycles.LONG_CYCLES)
  );

  public enum ID {
    CYCLE_REVIEW_REGULAR_CYCLES,
    CYCLE_REVIEW_LONG_CYCLES
  }

  public abstract ID id();
  public abstract List<TrainingCycle> trainingCycles();

  public static Exercise create(ID id, List<TrainingCycle> trainingCycles) {
    return new AutoValue_Exercise(id, trainingCycles);
  }

  public static Optional<Exercise> forID(ID id) {
    for (Exercise e : COLLECTION) {
      if (e.id() == id) {
        return Optional.of(e);
      }
    }
    return Optional.empty();
  }
}

package com.bloomcyclecare.cmcc.data.repos.cycle;

import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.models.TrainingCycles;

import org.joda.time.LocalDate;

import androidx.annotation.NonNull;

public class CycleRepos {

  public static RWCycleRepo forRoomDB(@NonNull AppDatabase database) {
    return new RoomCycleRepo(database);
  }

  public static RWCycleRepo forTraining() {
    return new TrainingCycleRepo(TrainingCycles.basicTrainingCycles(), LocalDate::now);
  }
}

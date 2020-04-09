package com.bloomcyclecare.cmcc.data.repos.instructions;

import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.models.TrainingCycles;

public class InstructionsRepos {

  public static RWInstructionsRepo forRoomDB(AppDatabase db) {
    return new RoomInstructionsRepo(db);
  }

  public static RWInstructionsRepo forTraining() {
    return new TrainingInstructionsRepo(TrainingCycles.forRepos());
  }
}

package com.bloomcyclecare.cmcc.data.repos.instructions;

import com.bloomcyclecare.cmcc.data.db.AppDatabase;

public class InstructionsRepos {

  public static RWInstructionsRepo forRoomDB(AppDatabase db) {
    return new RoomInstructionsRepo(db);
  }
}

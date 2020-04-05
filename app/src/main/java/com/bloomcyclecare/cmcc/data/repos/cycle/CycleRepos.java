package com.bloomcyclecare.cmcc.data.repos.cycle;

import com.bloomcyclecare.cmcc.data.db.AppDatabase;

import androidx.annotation.NonNull;

public class CycleRepos {

  public static RWCycleRepo forRoomDB(@NonNull AppDatabase database) {
    return new RoomCycleRepo(database);
  }
}

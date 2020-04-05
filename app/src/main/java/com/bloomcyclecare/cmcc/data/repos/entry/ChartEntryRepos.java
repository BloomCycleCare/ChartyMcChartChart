package com.bloomcyclecare.cmcc.data.repos.entry;

import com.bloomcyclecare.cmcc.data.db.AppDatabase;

import androidx.annotation.NonNull;

public class ChartEntryRepos {

  public static RWChartEntryRepo forRoomDB(@NonNull AppDatabase db) {
    return new RoomChartEntryRepo(db);
  }
}

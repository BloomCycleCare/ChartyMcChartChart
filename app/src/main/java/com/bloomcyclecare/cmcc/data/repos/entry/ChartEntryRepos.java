package com.bloomcyclecare.cmcc.data.repos.entry;

import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.models.TrainingCycles;
import com.bloomcyclecare.cmcc.logic.chart.ObservationParser;

import androidx.annotation.NonNull;

public class ChartEntryRepos {

  public static RWChartEntryRepo forRoomDB(@NonNull AppDatabase db) {
    return new RoomChartEntryRepo(db);
  }

  public static RWChartEntryRepo forTraining() throws ObservationParser.InvalidObservationException {
    return new TrainingChartEntryRepo(TrainingCycles.forRepos());
  }
}

package com.bloomcyclecare.cmcc.data.repos.pregnancy;

import com.bloomcyclecare.cmcc.data.db.AppDatabase;
import com.bloomcyclecare.cmcc.data.repos.cycle.RWCycleRepo;

public class PregnancyRepos {

  public static RWPregnancyRepo forRoomDb(AppDatabase db, RWCycleRepo cycleRepo) {
    return new RoomPregnancyRepo(db, cycleRepo);
  }
}

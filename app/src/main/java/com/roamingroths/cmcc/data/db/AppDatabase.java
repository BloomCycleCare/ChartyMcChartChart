package com.roamingroths.cmcc.data.db;


import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;

import com.google.common.collect.ImmutableList;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.data.entities.ObservationEntry;
import com.roamingroths.cmcc.data.entities.SymptomEntry;
import com.roamingroths.cmcc.data.entities.WellnessEntry;

import java.util.List;

@Database(
    entities = {
        Cycle.class,
        ObservationEntry.class,
        SymptomEntry.class,
        WellnessEntry.class,
    },
    version = 1)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract CycleDao cycleDao();
    public abstract ObservationEntryDao observationEntryDao();
    public abstract WellnessEntryDao wellnessEntryDao();
    public abstract SymptomEntryDao symptomEntryDao();

    public static List<Migration> MIGRATIONS = ImmutableList.of();
}

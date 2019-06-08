package com.roamingroths.cmcc.data.db;


import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.google.common.collect.ImmutableList;
import com.roamingroths.cmcc.data.entities.Cycle;
import com.roamingroths.cmcc.data.entities.Instructions;
import com.roamingroths.cmcc.data.entities.ObservationEntry;
import com.roamingroths.cmcc.data.entities.SymptomEntry;
import com.roamingroths.cmcc.data.entities.WellnessEntry;

import java.util.List;

@Database(
    entities = {
        Cycle.class,
        Instructions.class,
        ObservationEntry.class,
        SymptomEntry.class,
        WellnessEntry.class,
    },
    version = 6)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract CycleDao cycleDao();
    public abstract InstructionDao instructionDao();
    public abstract ObservationEntryDao observationEntryDao();
    public abstract WellnessEntryDao wellnessEntryDao();
    public abstract SymptomEntryDao symptomEntryDao();

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `Instructions` (`startDate` TEXT NOT NULL, `endDate` TEXT, `usePostPeakYellowStickers` INTEGER NOT NULL, `usePrePeakYellowStickers` INTEGER NOT NULL, `useSpecialSamenessYellowStickers` INTEGER NOT NULL, PRIMARY KEY(`startDate`))");
        }
    };

    public static List<Migration> MIGRATIONS = ImmutableList.of(MIGRATION_2_3);
}

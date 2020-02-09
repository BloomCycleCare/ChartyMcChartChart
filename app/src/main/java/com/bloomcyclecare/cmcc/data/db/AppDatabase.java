package com.bloomcyclecare.cmcc.data.db;


import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.bloomcyclecare.cmcc.data.entities.ObservationEntry;
import com.bloomcyclecare.cmcc.data.entities.SymptomEntry;
import com.bloomcyclecare.cmcc.data.entities.WellnessEntry;
import com.google.common.collect.ImmutableList;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
    entities = {
        Cycle.class,
        Instructions.class,
        ObservationEntry.class,
        SymptomEntry.class,
        WellnessEntry.class,
    },
    version = 9)
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

    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Instructions ADD yellowStampInstructions TEXT");
        }
    };

    private static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE ObservationEntry ADD note TEXT");
        }
    };
    private static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE ObservationEntry ADD unusualBuildup INTEGER NOT NULL DEFAULT (0)");
            database.execSQL("ALTER TABLE ObservationEntry ADD unusualStress INTEGER NOT NULL DEFAULT (0)");
        }
    };

    public static List<Migration> MIGRATIONS = ImmutableList.of(MIGRATION_2_3, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9);
}

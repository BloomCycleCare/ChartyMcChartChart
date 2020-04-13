package com.bloomcyclecare.cmcc.data.db;


import com.bloomcyclecare.cmcc.data.entities.Cycle;
import com.bloomcyclecare.cmcc.data.entities.Instructions;
import com.bloomcyclecare.cmcc.data.entities.ObservationEntry;
import com.bloomcyclecare.cmcc.data.entities.Pregnancy;
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
        Pregnancy.class,
    },
    version = 14)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract CycleDao cycleDao();
    public abstract InstructionDao instructionDao();
    public abstract ObservationEntryDao observationEntryDao();
    public abstract WellnessEntryDao wellnessEntryDao();
    public abstract SymptomEntryDao symptomEntryDao();
    public abstract PregnancyDao pregnancyDao();

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
    private static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE ObservationEntry ADD positivePregnancyTest INTEGER NOT NULL DEFAULT (0)");
        }
    };
    private static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add new pregnancy table
            database.execSQL("CREATE TABLE IF NOT EXISTS `Pregnancy` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `positiveTestDate` TEXT)");
            // Create temporary cycle table for adding foreign key
            database.execSQL("CREATE TABLE IF NOT EXISTS `Cycle_new` (`id` TEXT, `startDate` TEXT NOT NULL, `endDate` TEXT, `pregnancyId` INTEGER, PRIMARY KEY(`startDate`), FOREIGN KEY(`pregnancyId`) REFERENCES `Pregnancy`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            // Copy data
            database.execSQL("INSERT INTO `Cycle_new` (id, startDate, endDate) SELECT id, startDate, endDate FROM Cycle");
            database.execSQL("DROP TABLE Cycle");
            database.execSQL("ALTER TABLE Cycle_new RENAME TO Cycle");
        }
    };
    private static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Pregnancy ADD dueDate TEXT");
        }
    };
    private static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Pregnancy ADD deliveryDate TEXT");
        }
    };
    private static final Migration MIGRATION_13_14 = new Migration(13, 14) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_Cycle_pregnancyId` ON `Cycle` (`pregnancyId`)");
        }
    };

    public static List<Migration> MIGRATIONS = ImmutableList.of(
        MIGRATION_2_3, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11,
        MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14);
}

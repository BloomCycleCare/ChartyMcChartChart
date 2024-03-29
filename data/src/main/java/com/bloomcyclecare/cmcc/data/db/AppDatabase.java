package com.bloomcyclecare.cmcc.data.db;


import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.bloomcyclecare.cmcc.data.models.breastfeeding.BreastfeedingEntry;
import com.bloomcyclecare.cmcc.data.models.charting.Cycle;
import com.bloomcyclecare.cmcc.data.models.instructions.Instructions;
import com.bloomcyclecare.cmcc.data.models.medication.Prescription;
import com.bloomcyclecare.cmcc.data.models.wellbeing.WellbeingEntry;
import com.bloomcyclecare.cmcc.data.models.measurement.MeasurementEntry;
import com.bloomcyclecare.cmcc.data.models.medication.Medication;
import com.bloomcyclecare.cmcc.data.models.medication.MedicationRef;
import com.bloomcyclecare.cmcc.data.models.observation.ObservationEntry;
import com.bloomcyclecare.cmcc.data.models.pregnancy.Pregnancy;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelectionEntry;
import com.google.common.collect.ImmutableList;

import org.intellij.lang.annotations.Language;

import java.util.Arrays;
import java.util.List;

@Database(
    entities = {
        Cycle.class,
        Instructions.class,
        ObservationEntry.class,
        StickerSelectionEntry.class,
        MeasurementEntry.class,
        BreastfeedingEntry.class,
        Pregnancy.class,
        Medication.class,
        MedicationRef.class,
        WellbeingEntry.class,
        Prescription.class,
    },
    version = 31)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

  public abstract CycleDao cycleDao();

  public abstract InstructionDao instructionDao();

  public abstract ObservationEntryDao observationEntryDao();

  public abstract StickerSelectionEntryDao stickerSelectionEntryDao();

  public abstract MeasurementEntryDao measurementEntryDao();

  public abstract BreastfeedingEntryDao breastfeedingEntryDao();

  public abstract PregnancyDao pregnancyDao();

  public abstract MedicationDao medicationDao();

  public abstract WellbeingEntryDao lifestyleEntryDao();

  public abstract PrescriptionDao prescriptionDao();

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
  private static final Migration MIGRATION_14_15 = new Migration(14, 15) {
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase database) {
      database.execSQL("CREATE TABLE IF NOT EXISTS `StickerSelectionEntry` (`entryDate` TEXT NOT NULL, `selection` TEXT, PRIMARY KEY(`entryDate`))");
    }
  };

  private static final BwCompatMigration MIGRATION_15_16 = new BwCompatMigration(
      15, 16,
      QuerySet.of("CREATE TABLE IF NOT EXISTS `MeasurementEntry` (`entryDate` TEXT NOT NULL, `monitorReading` TEXT, `lhTestResult` TEXT, PRIMARY KEY(`entryDate`))"),
      QuerySet.of("DROP TABLE `MeasurementEntry`"));

  static final BwCompatMigration MIGRATION_16_17 = new BwCompatMigration(
      16, 17,
      QuerySet.of(
          "ALTER TABLE Pregnancy ADD COLUMN breastfeedingStartDate TEXT",
          "ALTER TABLE Pregnancy ADD COLUMN breastfeedingEndDate TEXT"
      ),
      QuerySet.of(
          "CREATE TABLE `Pregnancy_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `positiveTestDate` TEXT, `dueDate` TEXT, `deliveryDate` TEXT)",
          "INSERT INTO `Pregnancy_new` SELECT id, positiveTestDate, dueDate, deliveryDate FROM `Pregnancy`",
          "DROP TABLE `Pregnancy`",
          "ALTER TABLE `Pregnancy_new` RENAME TO `Pregnancy`"
      ));

  static final BwCompatMigration MIGRATION_17_18 = new BwCompatMigration(
      17, 18,
      QuerySet.of("CREATE TABLE IF NOT EXISTS `BreastfeedingEntry` (`entryDate` TEXT NOT NULL, `numDayFeedings` INTEGER NOT NULL, `numNightFeedings` INTEGER NOT NULL, `maxGapBetweenFeedings` TEXT, PRIMARY KEY(`entryDate`))"),
      QuerySet.of("DROP TABLE `BreastfeedingEntry`"));

  static final BwCompatMigration MIGRATION_18_19 = new BwCompatMigration(
      18, 19,
      QuerySet.of("ALTER TABLE Pregnancy ADD COLUMN babyDaybookName TEXT"),
      QuerySet.of(
          "CREATE TABLE `Pregnancy_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `positiveTestDate` TEXT, `dueDate` TEXT, `deliveryDate` TEXT, `breastfeedingStartDate` TEXT, `breastfeedingEndDate` TEXT)",
          "INSERT INTO `Pregnancy_new` SELECT id, positiveTestDate, dueDate, deliveryDate, breastfeedingStartDate, breastfeedingEndDate FROM `Pregnancy`",
          "DROP TABLE `Pregnancy`",
          "ALTER TABLE `Pregnancy_new` RENAME TO `Pregnancy`"
          ));

  static final BwCompatMigration MIGRATION_19_20 = new BwCompatMigration(
      19, 20,
      QuerySet.of(
          "ALTER TABLE ObservationEntry ADD COLUMN timeCreated INTEGER",
          "ALTER TABLE ObservationEntry ADD COLUMN timeUpdated INTEGER",
          "ALTER TABLE StickerSelectionEntry ADD COLUMN timeCreated INTEGER",
          "ALTER TABLE StickerSelectionEntry ADD COLUMN timeUpdated INTEGER",
          "ALTER TABLE SymptomEntry ADD COLUMN timeCreated INTEGER",
          "ALTER TABLE SymptomEntry ADD COLUMN timeUpdated INTEGER",
          "ALTER TABLE WellnessEntry ADD COLUMN timeCreated INTEGER",
          "ALTER TABLE WellnessEntry ADD COLUMN timeUpdated INTEGER",
          "ALTER TABLE MeasurementEntry ADD COLUMN timeCreated INTEGER",
          "ALTER TABLE MeasurementEntry ADD COLUMN timeUpdated INTEGER",
          "ALTER TABLE BreastfeedingEntry ADD COLUMN timeCreated INTEGER",
          "ALTER TABLE BreastfeedingEntry ADD COLUMN timeUpdated INTEGER"
      ),
      QuerySet.of());

  static final BwCompatMigration MIGRATION_20_21 = new BwCompatMigration(
      20, 21,
      QuerySet.of(
          "ALTER TABLE ObservationEntry ADD COLUMN timesUpdated INTEGER DEFAULT 0 NOT NULL",
          "ALTER TABLE StickerSelectionEntry ADD COLUMN timesUpdated INTEGER DEFAULT 0 NOT NULL",
          "ALTER TABLE SymptomEntry ADD COLUMN timesUpdated INTEGER DEFAULT 0 NOT NULL",
          "ALTER TABLE WellnessEntry ADD COLUMN timesUpdated INTEGER DEFAULT 0 NOT NULL",
          "ALTER TABLE MeasurementEntry ADD COLUMN timesUpdated INTEGER DEFAULT 0 NOT NULL",
          "ALTER TABLE BreastfeedingEntry ADD COLUMN timesUpdated INTEGER DEFAULT 0 NOT NULL"
      ),
      QuerySet.of());

  static final BwCompatMigration MIGRATION_21_22 = new BwCompatMigration(
      21, 22,
      QuerySet.of("ALTER TABLE ObservationEntry ADD COLUMN uncertain INTEGER NOT NULL DEFAULT (0)"),
      QuerySet.of());

  static final BwCompatMigration MIGRATION_22_23 = new BwCompatMigration(
      22, 23,
      QuerySet.of(
          "CREATE TABLE IF NOT EXISTS `MedicationRef` (`entryDate` TEXT NOT NULL, `medicationId` INTEGER NOT NULL, PRIMARY KEY(`entryDate`, `medicationId`), FOREIGN KEY(`medicationId`) REFERENCES `Medication`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
          "CREATE INDEX IF NOT EXISTS `index_MedicationRef_medicationId` ON `MedicationRef` (`medicationId`)",
          "CREATE TABLE IF NOT EXISTS `MedicationEntry` (`entryDate` TEXT NOT NULL, `timeCreated` INTEGER, `timeUpdated` INTEGER, `timesUpdated` INTEGER NOT NULL, PRIMARY KEY(`entryDate`))",
          "CREATE TABLE IF NOT EXISTS `Medication` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `description` TEXT, `dosage` TEXT, `frequency` TEXT, `active` INTEGER NOT NULL)"
      ),
      QuerySet.of(
          "DROP TABLE `MedicationRef`",
          "DROP TABLE `MedicationEntry`",
          "DROP TABLE `Medication`",
          "DROP INDEX `index_MedicationRef_medicationId`"
      ));

  static final BwCompatMigration MIGRATION_23_24 = new BwCompatMigration(
      23, 24,
      QuerySet.of("CREATE TABLE IF NOT EXISTS `WellbeingEntry` (`entryDate` TEXT NOT NULL, `timeCreated` INTEGER, `timeUpdated` INTEGER, `timesUpdated` INTEGER NOT NULL, `painObservationMorning` INTEGER, `painObservationAfternoon` INTEGER, `painObservationEvening` INTEGER, `painObservationNight` INTEGER, PRIMARY KEY(`entryDate`))"),
      QuerySet.of("DROP TABLE `WellbeingEntry`"));

  static final BwCompatMigration MIGRATION_24_25 = new BwCompatMigration(
      24, 25,
      QuerySet.of("DROP TABLE `WellnessEntry`", "DROP TABLE `SymptomEntry`"),
      QuerySet.of());

  static final BwCompatMigration MIGRATION_25_26 = new BwCompatMigration(
      25, 26,
      QuerySet.of("ALTER TABLE WellbeingEntry ADD COLUMN energyLevel INTEGER"),
      QuerySet.of());

  static final BwCompatMigration MIGRATION_26_27 = new BwCompatMigration(
      26, 27,
      QuerySet.of(
          "ALTER TABLE Medication ADD COLUMN takeInMorning INTEGER NOT NULL DEFAULT (0)",
          "ALTER TABLE Medication ADD COLUMN takeAtNoon INTEGER NOT NULL DEFAULT (0)",
          "ALTER TABLE Medication ADD COLUMN takeInEvening INTEGER NOT NULL DEFAULT (0)",
          "ALTER TABLE Medication ADD COLUMN takeAtNight INTEGER NOT NULL DEFAULT (0)"),
      QuerySet.of());

  static final BwCompatMigration MIGRATION_27_28 = new BwCompatMigration(
      27, 28,
      QuerySet.of("ALTER TABLE MedicationRef ADD COLUMN time TEXT"),
      QuerySet.of());

  static final BwCompatMigration MIGRATION_28_29 = new BwCompatMigration(
      28, 29,
      QuerySet.of(
          "DROP TABLE MedicationEntry",
          "DROP TABLE MedicationRef",
          "CREATE TABLE IF NOT EXISTS `MedicationRef` (`entryDate` TEXT NOT NULL, `medicationId` INTEGER NOT NULL, `time` TEXT NOT NULL, PRIMARY KEY(`entryDate`, `medicationId`, `time`), FOREIGN KEY(`medicationId`) REFERENCES `Medication`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
          "CREATE INDEX IF NOT EXISTS `index_MedicationRef_medicationId` ON `MedicationRef` (`medicationId`)"
      ), QuerySet.of());

  static final BwCompatMigration MIGRATION_29_30 = new BwCompatMigration(
      29, 30,
      QuerySet.of(
          "CREATE TABLE IF NOT EXISTS `Medication_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `description` TEXT, `dosage` TEXT, `takeInMorning` INTEGER NOT NULL, `takeAtNoon` INTEGER NOT NULL, `takeInEvening` INTEGER NOT NULL, `takeAtNight` INTEGER NOT NULL, `takeAsNeeded` INTEGER NOT NULL)",
          "INSERT INTO `Medication_new` SELECT id, name, description, dosage, takeInMorning, takeAtNoon, takeInEvening, takeAtNight, active AND NOT (takeInMorning OR takeAtNoon OR takeInEvening OR takeAtNight) AS takeAsNeeded FROM `Medication`",
          "DROP TABLE `Medication`",
          "ALTER TABLE `Medication_new` RENAME TO `Medication`"
      ), QuerySet.of());

  static final BwCompatMigration MIGRATION_30_31 = new BwCompatMigration(
      30, 31,
      QuerySet.of(
          "CREATE TABLE IF NOT EXISTS `Prescription` (`medicationId` INTEGER NOT NULL, `startDate` TEXT NOT NULL, `endDate` TEXT, `dosage` TEXT, `takeInMorning` INTEGER NOT NULL, `takeAtNoon` INTEGER NOT NULL, `takeInEvening` INTEGER NOT NULL, `takeAtNight` INTEGER NOT NULL, `takeAsNeeded` INTEGER NOT NULL, PRIMARY KEY(`medicationId`, `startDate`), FOREIGN KEY(`medicationId`) REFERENCES `Medication`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
          "CREATE INDEX IF NOT EXISTS `index_Prescription_medicationId` ON `Prescription` (`medicationId`)",
          "CREATE TABLE IF NOT EXISTS `Medication_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `description` TEXT)",
          "INSERT INTO `Medication_new` SELECT id, name, description FROM `Medication`",
          "DROP TABLE `Medication`",
          "ALTER TABLE `Medication_new` RENAME TO `Medication`"
      ),
      QuerySet.of("DROP TABLE `Prescription`"));

  public static List<Migration> MIGRATIONS = ImmutableList.<Migration>builder()
      .add(MIGRATION_2_3, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
          MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
      .addAll(MIGRATION_15_16.migrations())
      .addAll(MIGRATION_16_17.migrations())
      .addAll(MIGRATION_17_18.migrations())
      .addAll(MIGRATION_18_19.migrations())
      .addAll(MIGRATION_19_20.migrations())
      .addAll(MIGRATION_20_21.migrations())
      .addAll(MIGRATION_21_22.migrations())
      .addAll(MIGRATION_22_23.migrations())
      .addAll(MIGRATION_23_24.migrations())
      .addAll(MIGRATION_24_25.migrations())
      .addAll(MIGRATION_25_26.migrations())
      .addAll(MIGRATION_26_27.migrations())
      .addAll(MIGRATION_27_28.migrations())
      .addAll(MIGRATION_28_29.migrations())
      .addAll(MIGRATION_29_30.migrations())
      .addAll(MIGRATION_30_31.migrations())
      .build();

  public static class QuerySet {
    public final ImmutableList<String> queries;

    private QuerySet(@Language("RoomSql") String... query) {
      queries = ImmutableList.<String>builder().addAll(Arrays.asList(query)).build();
    }

    static QuerySet of(@Language("RoomSql") String... query) {
      return new QuerySet(query);
    }
  }

  static class BwCompatMigration {
    public final int fromVersion;
    public final int toVersion;
    public final QuerySet forwardSQL;
    public final QuerySet backwardSQL;

    private BwCompatMigration(int fromVersion, int toVersion, QuerySet forwardSQL, QuerySet backwardSQL) {
      this.fromVersion = fromVersion;
      this.toVersion = toVersion;
      this.forwardSQL = forwardSQL;
      this.backwardSQL = backwardSQL;
    }

    public Migration forwardMigration() {
      return new Migration(fromVersion, toVersion) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          database.beginTransaction();
          try {
            for (String query : forwardSQL.queries) {
              database.execSQL(query);
            }
            database.setTransactionSuccessful();
          } finally {
            database.endTransaction();
          }
        }
      };
    }

    public Migration backwardMigration() {
      return new Migration(toVersion, fromVersion) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
          database.beginTransaction();
          try {
            for (String query : backwardSQL.queries) {
              database.execSQL(query);
            }
            database.setTransactionSuccessful();
          } finally {
            database.endTransaction();
          }
        }
      };
    }

    public ImmutableList<Migration> migrations() {
      return ImmutableList.of(forwardMigration(), backwardMigration());
    }
  }

}

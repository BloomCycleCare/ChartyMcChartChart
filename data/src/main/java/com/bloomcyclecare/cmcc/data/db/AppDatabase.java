package com.bloomcyclecare.cmcc.data.db;


import com.bloomcyclecare.cmcc.data.models.breastfeeding.BreastfeedingEntry;
import com.bloomcyclecare.cmcc.data.models.charting.Cycle;
import com.bloomcyclecare.cmcc.data.models.instructions.Instructions;
import com.bloomcyclecare.cmcc.data.models.measurement.MeasurementEntry;
import com.bloomcyclecare.cmcc.data.models.observation.ObservationEntry;
import com.bloomcyclecare.cmcc.data.models.observation.SymptomEntry;
import com.bloomcyclecare.cmcc.data.models.observation.WellnessEntry;
import com.bloomcyclecare.cmcc.data.models.pregnancy.Pregnancy;
import com.bloomcyclecare.cmcc.data.models.stickering.StickerSelectionEntry;
import com.google.common.collect.ImmutableList;

import org.intellij.lang.annotations.Language;

import java.util.Arrays;
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
        StickerSelectionEntry.class,
        SymptomEntry.class,
        WellnessEntry.class,
        MeasurementEntry.class,
        BreastfeedingEntry.class,
        Pregnancy.class,
    },
    version = 19)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

  public abstract CycleDao cycleDao();

  public abstract InstructionDao instructionDao();

  public abstract ObservationEntryDao observationEntryDao();

  public abstract WellnessEntryDao wellnessEntryDao();

  public abstract StickerSelectionEntryDao stickerSelectionEntryDao();

  public abstract SymptomEntryDao symptomEntryDao();

  public abstract MeasurementEntryDao measurementEntryDao();

  public abstract BreastfeedingEntryDao breastfeedingEntryDao();

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

  public static List<Migration> MIGRATIONS = ImmutableList.<Migration>builder()
      .add(MIGRATION_2_3, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
          MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
      .addAll(MIGRATION_15_16.migrations())
      .addAll(MIGRATION_16_17.migrations())
      .addAll(MIGRATION_17_18.migrations())
      .addAll(MIGRATION_18_19.migrations())
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

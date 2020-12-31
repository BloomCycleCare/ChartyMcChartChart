package com.bloomcyclecare.cmcc.data.db;

import android.database.Cursor;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
public class AppDatabaseTest {

  private static final String TEST_DB = "migration-test";

  @Rule
  public MigrationTestHelper helper;

  public AppDatabaseTest() {
    helper = new MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
        AppDatabase.class.getCanonicalName(),
        new FrameworkSQLiteOpenHelperFactory());
  }

  @Test
  public void migrateAll() throws IOException {
    // Create earliest version of the database.
    SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 15);
    db.close();

    // Open latest version of the database. Room will validate the schema
    // once all migrations execute.
    AppDatabase appDb = Room.databaseBuilder(
        InstrumentationRegistry.getInstrumentation().getTargetContext(),
        AppDatabase.class,
        TEST_DB)
        .addMigrations(AppDatabase.MIGRATIONS.toArray(new Migration[0])).build();
    appDb.getOpenHelper().getWritableDatabase();
    appDb.close();
  }

  @Test
  public void downgrade17to16() throws IOException {
    SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 17);

    db.execSQL("INSERT INTO Pregnancy (id, positiveTestDate, dueDate, deliveryDate, breastfeedingStartDate) VALUES (1, '2020-01-01', '2020-12-01', '2020-12-05', '2020-12-05')");
    db.close();

    db = helper.runMigrationsAndValidate(TEST_DB, 16, true, AppDatabase.MIGRATION_16_17.backwardMigration());

    Cursor c = db.query("SELECT * FROM Pregnancy");
    assertThat(c.getCount()).isEqualTo(1);
    assertThat(c.getColumnCount()).isEqualTo(4);

    assertThat(c.moveToFirst()).isTrue();
    assertThat(c.getInt(c.getColumnIndexOrThrow("id")))
        .isEqualTo(1);
    assertThat(c.getString(c.getColumnIndexOrThrow("positiveTestDate")))
        .isEqualTo("2020-01-01");
    assertThat(c.getString(c.getColumnIndexOrThrow("dueDate")))
        .isEqualTo("2020-12-01");
    assertThat(c.getString(c.getColumnIndexOrThrow("deliveryDate")))
        .isEqualTo("2020-12-05");
  }

  @Test
  public void downgrade19to18() throws IOException {
    SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 19);

    db.execSQL("INSERT INTO Pregnancy (id, positiveTestDate, dueDate, deliveryDate, breastfeedingStartDate, babyDaybookName) VALUES (1, '2020-01-01', '2020-12-01', '2020-12-05', '2020-12-05', 'foo')");
    db.close();

    db = helper.runMigrationsAndValidate(TEST_DB, 18, true, AppDatabase.MIGRATION_18_19.backwardMigration());

    Cursor c = db.query("SELECT * FROM Pregnancy");
    assertThat(c.getCount()).isEqualTo(1);
    assertThat(c.getColumnCount()).isEqualTo(6);

    assertThat(c.moveToFirst()).isTrue();
    assertThat(c.getInt(c.getColumnIndexOrThrow("id")))
        .isEqualTo(1);
    assertThat(c.getString(c.getColumnIndexOrThrow("positiveTestDate")))
        .isEqualTo("2020-01-01");
    assertThat(c.getString(c.getColumnIndexOrThrow("dueDate")))
        .isEqualTo("2020-12-01");
    assertThat(c.getString(c.getColumnIndexOrThrow("deliveryDate")))
        .isEqualTo("2020-12-05");
    assertThat(c.getString(c.getColumnIndexOrThrow("breastfeedingStartDate")))
        .isEqualTo("2020-12-05");
  }
}

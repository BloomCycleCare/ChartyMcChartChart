package com.roamingroths.cmcc.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.roamingroths.cmcc.data.ObservationContract.ObservationEntry;

public class DbHelper extends SQLiteOpenHelper {

  public static final String DATABASE_NAME = "cmcc.db";
  private static final int DATABASE_VERSION = 4;

  public DbHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {

    final String SQL_CREATE_OBSERVATION_TABLE = new StringBuilder()
        .append("CREATE TABLE ").append(ObservationEntry.TABLE_NAME).append("(")
        .append(ObservationEntry._ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
        .append(ObservationEntry.COLUMN_CREATION_DATE).append(" INTEGER NOT NULL, ")
        .append(ObservationEntry.COLUMN_FLOW).append(" REAL NOT NULL, ")
        .append(ObservationEntry.COLUMN_OCCURENCE).append(" REAL NOT NULL, ")
        .append(" UNIQUE (" + ObservationEntry.COLUMN_CREATION_DATE + ") ON CONFLICT REPLACE);")
        .toString();
    db.execSQL(SQL_CREATE_OBSERVATION_TABLE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.execSQL("DROP TABLE IF EXISTS " + ObservationEntry.TABLE_NAME);
    onCreate(db);
  }
}

package com.roamingroths.cmcc.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

  public DbHelper(Context context) {
    super(context, DbContract.DATABASE_NAME, null, DbContract.DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(DbContract.ChartEntryTable.CREATE_TABLE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.execSQL(DbContract.ChartEntryTable.DELETE_TABLE);
    onCreate(db);
  }
}

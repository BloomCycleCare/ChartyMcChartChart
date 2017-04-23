package com.roamingroths.cmcc.data;

import android.provider.BaseColumns;

/**
 * Created by parkeroth on 4/23/17.
 */

public final class DbContract {
  public static final String DATABASE_NAME = "cmcc.db";
  public static final int DATABASE_VERSION = 4;

  // Do not instantiate
  private DbContract() {
  }

  public static abstract class ChartEntryTable implements BaseColumns {
    public static final String TABLE_NAME = "observation";

    public static final String COLUMN_CREATION_DATE = "creation_date";
    public static final String COLUMN_OBSERVATION_FLOW = "observation_flow";
    public static final String COLUMN_OBSERVATION_DISCHARGE_TYPE = "observation_discharge_type";
    public static final String COLUMN_OBSERVATION_DISCHARGE_MODIFIERS = "observation_discharge_modifiers";
    public static final String COLUMN_OBSERVATION_OCCURRENCE = "observation_occurrence";
    public static final String COLUMN_PEAK_DAY = "peak_day";
    public static final String COLUMN_INTERCOURSE = "intercourse";

    public static final String CREATE_TABLE = new StringBuilder()
        .append("CREATE TABLE ").append(TABLE_NAME).append("(")
        .append(_ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ")
        .append(COLUMN_CREATION_DATE).append(" INTEGER NOT NULL, ")
        .append(COLUMN_PEAK_DAY).append(" INTEGER NOT NULL, ")
        .append(COLUMN_INTERCOURSE).append(" INTEGER NOT NULL, ")
        .append(COLUMN_OBSERVATION_FLOW).append(" REAL, ")
        .append(COLUMN_OBSERVATION_DISCHARGE_TYPE).append(" REAL, ")
        .append(COLUMN_OBSERVATION_DISCHARGE_MODIFIERS).append(" REAL, ")
        .append(COLUMN_OBSERVATION_OCCURRENCE).append(" REAL, ")
        .append(" UNIQUE (" + COLUMN_CREATION_DATE + ") ON CONFLICT REPLACE);")
        .toString();

    public static final String DELETE_TABLE = new StringBuilder()
        .append("DROP TABLE IF EXISTS ").append(TABLE_NAME).toString();
  }
}

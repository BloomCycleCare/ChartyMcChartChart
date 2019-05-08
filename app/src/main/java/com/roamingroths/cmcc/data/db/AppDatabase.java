package com.roamingroths.cmcc.data.db;


import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.roamingroths.cmcc.data.entities.ObservationEntry;

@Database(
    entities = {
        ObservationEntry.class,
    },
    version = 1)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
}

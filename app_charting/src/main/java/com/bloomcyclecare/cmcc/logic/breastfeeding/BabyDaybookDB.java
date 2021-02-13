package com.bloomcyclecare.cmcc.logic.breastfeeding;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

import com.google.common.collect.ImmutableList;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.reactivex.Maybe;
import io.reactivex.Single;
import timber.log.Timber;

public class BabyDaybookDB {

  public static final String MIME_TYPE = "application/octet-stream";

  private final SQLiteDatabase mDB;

  private BabyDaybookDB(SQLiteDatabase db) {
    mDB = db;
  }

  public Single<List<ActionInterval>> actionIntervals(String babyName, String... types) {
    return babyId(babyName).toSingle()
        .flatMap(babyUID -> actionIntervalsForBabyID(babyUID, types));
  }

  private Single<ImmutableList<ActionInterval>> actionIntervalsForBabyID(String babyID, String... types) {
    return Single.create(emitter -> {
      String inClause = TextUtils.join(",", Collections.nCopies(types.length, "?"));
      String[] args = new String[types.length + 1];
      System.arraycopy(types, 0, args, 0, types.length);
      args[args.length - 1] = babyID;
      Cursor c = mDB.rawQuery("SELECT start_millis, end_millis, type FROM daily_actions WHERE type IN (" + inClause + ") AND baby_uid=? ORDER BY start_millis", args, null);

      ImmutableList.Builder<ActionInterval> out = ImmutableList.builder();
      while (c.moveToNext()) {
        DateTime start = new DateTime(c.getLong(0), DateTimeZone.getDefault());
        String type = c.getString(2);
        DateTime end;
        long endMillis = c.getLong(1);
        if (endMillis > 0) {
          end = new DateTime(endMillis, DateTimeZone.getDefault());
        } else if (type.equals("pump")){
          end = start.plusMinutes(1);
        } else {
          Timber.w("Skipping entry missing end time {type: %s, start: %s}", type, start);
          continue;
        }
        if (start.isAfter(end)) {
          Timber.w("Skipping malformed interval start (%s) end (%s)", start, end);
          continue;
        }
        out.add(new ActionInterval(type, new Interval(start, end)));
      }
      ImmutableList<ActionInterval> is = out.build();
      Timber.v("Found %d entries for %s", is.size(), babyID);
      emitter.onSuccess(is);
    });
  }

  private Maybe<String> babyId(String babyName) {
    return Maybe.create(emitter -> {
      String[] args = {babyName};
      Cursor c = mDB.rawQuery("SELECT uid FROM babies WHERE name=?", args, null);

      if (c.getCount() == 0) {
        Timber.d("No babies found");
        emitter.onComplete();
      } else if (c.getCount() > 1) {
        Timber.d("Several IDs found for %s", babyName);
        emitter.onComplete();
      } else if (!c.moveToFirst()) {
        Timber.d("Failed updating cursor");
        emitter.onComplete();
      } else {
        String id = c.getString(0);
        Timber.v("Found ID %s for baby name %s", id, babyName);
        emitter.onSuccess(id);
      }
    });
  }

  public static Single<BabyDaybookDB> fromIntent(Intent intent, Context context) {
    if (!intent.getType().equals(MIME_TYPE)) {
      return Single.error(new IllegalArgumentException("Unsupported mime type: " + intent.getType()));
    }
    Uri uri = intent.getData();
    if (uri == null) {
      return Single.error(new IllegalArgumentException("Intent had no data"));
    } else {
      return Single.create(emitter -> {
        File tmp = File.createTempFile("bbdb-", ".db", context.getCacheDir());
        OutputStream outputStream = new FileOutputStream(tmp);
        InputStream inputStream = context.getContentResolver().openInputStream(uri);

        byte[] buffer = new byte[1024];
        while (true) {
          int bytesRead = inputStream.read(buffer);
          if (bytesRead == -1) {
            break;
          }
          outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        outputStream.close();

        String path = tmp.getAbsolutePath();
        SQLiteDatabase db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
        emitter.onSuccess(new BabyDaybookDB(db));
      });
    }
 }

 public static class ActionInterval implements Comparable<ActionInterval> {
   public final String type;
   public final Interval interval;

   public ActionInterval(String type, Interval interval) {
     this.type = type;
     this.interval = interval;
   }

   @Override
   public String toString() {
     return "ActionInterval{" +
         "type='" + type + '\'' +
         ", interval=" + interval +
         '}';
   }

   @Override
   public boolean equals(Object o) {
     if (this == o) return true;
     if (o == null || getClass() != o.getClass()) return false;
     ActionInterval that = (ActionInterval) o;
     return type.equals(that.type) &&
         interval.equals(that.interval);
   }

   @Override
   public int hashCode() {
     return Objects.hash(type, interval);
   }

   @Override
   public int compareTo(ActionInterval that) {
     return this.interval.getStart().compareTo(that.interval.getStart());
   }
 }
}

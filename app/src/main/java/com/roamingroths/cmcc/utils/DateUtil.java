package com.roamingroths.cmcc.utils;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.annotations.Nullable;
import timber.log.Timber;

/**
 * Created by parkeroth on 5/14/17.
 */

public class DateUtil {

  private static final DateTimeFormatter WIRE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd");
  private static final DateTimeFormatter FILE_FORMAT = DateTimeFormat.forPattern("yyyyMMdd");
  private static final DateTimeFormatter UI_FORMAT = DateTimeFormat.forPattern("EEE d MMM");
  private static final DateTimeFormatter NEW_UI_FORMAT = DateTimeFormat.forPattern("d MMM yyyy");
  private static final DateTimeFormatter PRINT_UI_FORMAT = DateTimeFormat.forPattern("d MMM");
  private static final DateTimeFormatter PRINT_FORMAT = DateTimeFormat.forPattern("MM/dd");

  public static String toWireStr(LocalDate date) {
    if (date == null) {
      return null;
    }
    return WIRE_FORMAT.print(date);
  }

  public static String toFileStr(LocalDate date) {
    if (date == null) {
      return null;
    }
    return FILE_FORMAT.print(date);
  }

  public static String toUiStr(LocalDate date) {
    if (date == null) {
      return null;
    }
    return UI_FORMAT.print(date);
  }

  public static String toNewUiStr(LocalDate date) {
    if (date == null) {
      return null;
    }
    return NEW_UI_FORMAT.print(date);
  }

  public static String toPrintUiStr(LocalDate date) {
    if (date == null) {
      return null;
    }
    return PRINT_UI_FORMAT.print(date);
  }

  public static String toPrintStr(LocalDate date) {
    if (date == null) {
      return null;
    }
    return PRINT_FORMAT.print(date);
  }

  public static LocalDate fromWireStr(String dateStr) {
    if (Strings.isNullOrEmpty(dateStr)) {
      return null;
    }
    return WIRE_FORMAT.parseLocalDate(dateStr);
  }

  public static Flowable<LocalDate> nowStream() {
    long millisToMidnight = LocalDate.now().plusDays(1).toDate().getTime() - DateTime.now().getMillis();
    return Flowable.interval(millisToMidnight, 1, TimeUnit.DAYS)
        .map(i -> LocalDate.now())
        .startWith(LocalDate.now())
        .doOnNext(now -> Timber.v("Now: %s", now));
  }

  public static List<LocalDate> daysBetween(LocalDate firstDay, @Nullable LocalDate lastDay, boolean reversed) {
    List<LocalDate> dates = new ArrayList<>();
    LocalDate end = lastDay == null ? LocalDate.now() : lastDay;
    for (LocalDate date = firstDay; date.isBefore(end.plusDays(1)); date = date.plusDays(1)) {
      dates.add(date);
    }
    return reversed ? Lists.reverse(dates) : dates;
  }

  public static LocalDate now() {
    return LocalDate.now();
  }
}

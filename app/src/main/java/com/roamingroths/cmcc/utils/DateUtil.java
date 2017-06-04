package com.roamingroths.cmcc.utils;

import com.google.common.base.Strings;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by parkeroth on 5/14/17.
 */

public class DateUtil {

  private static final DateTimeFormatter WIRE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd");

  public static String toWireStr(LocalDate date) {
    if (date == null) {
      return null;
    }
    return WIRE_FORMAT.print(date);
  }

  public static LocalDate fromWireStr(String dateStr) {
    if (Strings.isNullOrEmpty(dateStr)) {
      return null;
    }
    return WIRE_FORMAT.parseLocalDate(dateStr);
  }

  public static Iterator<LocalDate> iterator(LocalDate firstDay, LocalDate lastDay) {
    List<LocalDate> dates = new ArrayList<>();
    for (LocalDate date = firstDay; date.isBefore(lastDay.plusDays(1)); date = date.plusDays(1)) {
      dates.add(date);
    }
    return dates.iterator();
  }

  public static LocalDate now() {
    return LocalDate.now();
  }
}

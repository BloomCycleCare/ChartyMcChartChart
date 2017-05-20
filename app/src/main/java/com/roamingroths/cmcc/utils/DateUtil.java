package com.roamingroths.cmcc.utils;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Created by parkeroth on 5/14/17.
 */

public class DateUtil {

  private static final String PATTERN = "yyyy-MM-dd";
  private static final DateTimeFormatter FORMAT = DateTimeFormat.forPattern(PATTERN);

  public static String toWireStr(LocalDate date) {
    return FORMAT.print(date);
  }

  public static LocalDate fromWireStr(String dateStr) {
    return FORMAT.parseLocalDate(dateStr);
  }

  public static LocalDate now() {
    return LocalDate.now();
  }
}

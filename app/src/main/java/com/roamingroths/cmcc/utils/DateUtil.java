package com.roamingroths.cmcc.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by parkeroth on 5/14/17.
 */

public class DateUtil {

  private static final SimpleDateFormat WIRE_DATE_FORMAT = new SimpleDateFormat("yyy-MM-dd");

  public static String toWireStr(Date date) {
    return WIRE_DATE_FORMAT.format(date);
  }

  public static Date fromWireStr(String dateStr) throws ParseException {
    return WIRE_DATE_FORMAT.parse(dateStr);
  }

  public static Date now() {
    try {
      return fromWireStr(toWireStr(new Date()));
    } catch (ParseException pe) {
      throw new IllegalStateException(pe);
    }
  }
}

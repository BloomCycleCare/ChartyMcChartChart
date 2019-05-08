package com.roamingroths.cmcc.data.domain;

public enum IntercourseTimeOfDay {
  NONE("None"), ANY("Any time of day"), END("End of day");

  final String value;

  IntercourseTimeOfDay(String value) {
    this.value = value;
  }

  public static IntercourseTimeOfDay fromStr(String str) {
    for (IntercourseTimeOfDay item : IntercourseTimeOfDay.values()) {
      if (item.value.equals(str)) {
        return item;
      }
    }
    throw new IllegalArgumentException(str + ": does not match any values");
  }
}

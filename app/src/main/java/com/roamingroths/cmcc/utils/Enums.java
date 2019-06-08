package com.roamingroths.cmcc.utils;

public class Enums {
  public static <E extends Enum<E>> E fromCode(int code, Class<E> clazz) {
    if (code < 0 || code > clazz.getEnumConstants().length) {
      throw new IllegalStateException();
    }
    return clazz.getEnumConstants()[code];
  }
}

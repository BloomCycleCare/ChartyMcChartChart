package com.roamingroths.cmcc.utils;

/**
 * Created by parkeroth on 4/20/17.
 */

public class StringUtil {

  public static String consumePrefix(String str, String prefix) {
    if (!str.startsWith(prefix)) {
      throw new IllegalArgumentException(str + " does not start with " + prefix);
    }
    return str.substring(prefix.length());
  }
}

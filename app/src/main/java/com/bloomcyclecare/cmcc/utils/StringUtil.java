package com.bloomcyclecare.cmcc.utils;

import java.util.Set;
import java.util.function.Predicate;

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

  public static <E extends Enum<E>> String consumeEnum(
      String str, Set<E> modifiers, Class<E> enumClazz, Predicate<E> pred) {
    for (E e : enumClazz.getEnumConstants()) {
      if (!pred.test(e)) {
        continue;
      }
      if (str.startsWith(e.name())) {
        modifiers.add(e);
        return consumeEnum(str.substring(e.name().length()), modifiers, enumClazz);
      }
    }
    return str;
  }

  public static <E extends Enum<E>> String consumeEnum(
      String str, Set<E> modifiers, Class<E> enumClazz) {
    return consumeEnum(str, modifiers, enumClazz, e -> true);
  }
}

package com.roamingroths.cmcc.crypto;

import java.security.Key;

/**
 * Created by parkeroth on 9/2/17.
 */

public interface Operation<K extends Key> {
  String apply(K key, String payload);
}

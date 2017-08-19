package com.roamingroths.cmcc.utils;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 8/18/17.
 */

public interface Cipherable {
  SecretKey getKey();

  void swapKey(SecretKey key);
}

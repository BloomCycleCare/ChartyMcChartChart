package com.roamingroths.cmcc.crypto;

/**
 * Created by parkeroth on 10/1/17.
 */

public class CyrptoExceptions {
  public static class CryptoException extends Exception {
    public CryptoException(Exception e) {
      super(e);
    }
  }
}

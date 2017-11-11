package com.roamingroths.cmcc.crypto;

import javax.crypto.SecretKey;

import io.reactivex.Maybe;
import io.reactivex.Single;

/**
 * Created by parkeroth on 10/15/17.
 */

public interface CryptoUtil {

  <T> Single<T> decrypt(String encryptedStr, SecretKey key, final Class<T> clazz);

  <T> Single<String> encrypt(Cipherable cipherable);

  Maybe<SecretKey> decryptKey(String encryptedKeyStr);

  Maybe<String> encryptKey(SecretKey key);
}

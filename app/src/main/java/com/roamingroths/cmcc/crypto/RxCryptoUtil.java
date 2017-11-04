package com.roamingroths.cmcc.crypto;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;

import io.reactivex.Maybe;

/**
 * Created by parkeroth on 10/15/17.
 */

public class RxCryptoUtil {

  private static final boolean DEBUG = true;
  private static final String TAG = RxCryptoUtil.class.getSimpleName();

  private final PrivateKey mPrivateKey;
  private final PublicKey mPublicKey;

  public RxCryptoUtil(KeyPair keyPair) {
    mPrivateKey = keyPair.getPrivate();
    mPublicKey = keyPair.getPublic();
  }

  public Maybe<SecretKey> decryptKey(String encryptedKeyStr) {
    return Maybe.fromCallable(RsaCryptoUtil.decrypt(mPrivateKey, encryptedKeyStr))
        .map(AesCryptoUtil.keyParser());
  }

  public Maybe<String> encryptKey(SecretKey key) {
    return Maybe.fromCallable(RsaCryptoUtil.encrypt(mPublicKey, AesCryptoUtil.serializeKey(key)));
  }
}

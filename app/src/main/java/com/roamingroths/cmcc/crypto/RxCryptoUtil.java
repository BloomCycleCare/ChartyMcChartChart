package com.roamingroths.cmcc.crypto;

import com.roamingroths.cmcc.utils.GsonUtil;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.Function;

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

  public <T> Single<T> decrypt(String encryptedStr, SecretKey key, final Class<T> clazz) {
    return AesCryptoUtil.decryptRx(key, encryptedStr)
        .map(new Function<String, T>() {
          @Override
          public T apply(String decryptedStr) throws Exception {
            return GsonUtil.getGsonInstance().fromJson(decryptedStr, clazz);
          }
        });
  }

  public <T> Single<String> encrypt(Cipherable cipherable) {
    return AesCryptoUtil.encryptRx(cipherable.getKey(), GsonUtil.getGsonInstance().toJson(cipherable));
  }

  public Maybe<SecretKey> decryptKey(String encryptedKeyStr) {
    return Maybe.fromCallable(RsaCryptoUtil.decrypt(mPrivateKey, encryptedKeyStr))
        .map(AesCryptoUtil.keyParser());
  }

  public Maybe<String> encryptKey(SecretKey key) {
    return Maybe.fromCallable(RsaCryptoUtil.encrypt(mPublicKey, AesCryptoUtil.serializeKey(key)));
  }
}

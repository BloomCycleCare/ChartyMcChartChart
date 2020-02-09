package com.bloomcyclecare.cmcc.crypto;

import android.util.Log;

import com.bloomcyclecare.cmcc.utils.GsonUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;

/**
 * Created by parkeroth on 10/15/17.
 */

public class BaseCryptoUtil implements CryptoUtil {

  private static final boolean DEBUG = false;
  private static final String TAG = BaseCryptoUtil.class.getSimpleName();

  private final PrivateKey mPrivateKey;
  private final PublicKey mPublicKey;

  private static final Cache<Integer, Object> OBJECT_CACHE =
      CacheBuilder.newBuilder().maximumSize(100).build();

  public BaseCryptoUtil(KeyPair keyPair) {
    mPrivateKey = keyPair.getPrivate();
    mPublicKey = keyPair.getPublic();
  }

  @Override
  public <T extends Cipherable> Single<T> decrypt(String encryptedStr, final SecretKey key, final Class<T> clazz) {
    if (DEBUG) Log.v(TAG, "Decrypting " + clazz.getSimpleName());
    return AesCryptoUtil.decryptRx(key, encryptedStr)
        .map(new Function<String, T>() {
          @Override
          public T apply(String decryptedStr) throws Exception {
            T t = GsonUtil.getGsonInstance().fromJson(decryptedStr, clazz);
            t.swapKey(key);
            return t;
          }
        });
  }

  @Override
  public Single<String> encrypt(Cipherable cipherable) {
    return AesCryptoUtil.encryptRx(cipherable.getKey(), GsonUtil.getGsonInstance().toJson(cipherable));
  }

  @Override
  public Function<Cipherable, SingleSource<String>> encrypt() {
    return new Function<Cipherable, SingleSource<String>>() {
      @Override
      public SingleSource<String> apply(Cipherable cipherable) throws Exception {
        return encrypt(cipherable);
      }
    };
  }

  @Override
  public Maybe<SecretKey> decryptKey(String encryptedKeyStr) {
    return Maybe.fromCallable(RsaCryptoUtil.decrypt(mPrivateKey, encryptedKeyStr))
        .map(AesCryptoUtil.keyParser());
  }

  @Override
  public Maybe<String> encryptKey(SecretKey key) {
    return Maybe.fromCallable(RsaCryptoUtil.encrypt(mPublicKey, AesCryptoUtil.serializeKey(key)));
  }
}

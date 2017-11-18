package com.roamingroths.cmcc.crypto;

import android.util.Log;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.security.KeyPair;

import javax.crypto.SecretKey;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;

/**
 * Created by parkeroth on 11/11/17.
 */

public class CachingCryptoUtil implements CryptoUtil {

  private static final boolean DEBUG = false;
  private static final String TAG = CachingCryptoUtil.class.getSimpleName();

  private static final Cache<Integer, Object> OBJECT_CACHE =
      CacheBuilder.newBuilder().maximumSize(100).build();

  private final CryptoUtil mDelegate;

  public CachingCryptoUtil(KeyPair keyPair) {
    this(new BaseCryptoUtil(keyPair));
  }

  CachingCryptoUtil(CryptoUtil delegate) {
    mDelegate = delegate;
  }

  @Override
  public <T extends Cipherable> Single<T> decrypt(final String encryptedStr, SecretKey key, Class<T> clazz) {
    Object cachedObject = OBJECT_CACHE.getIfPresent(encryptedStr.hashCode());
    if (cachedObject != null) {
      if (DEBUG) Log.v(TAG, "Served " + clazz.getName() + " from local cache");
      return Single.just((T) cachedObject);
    }
    return mDelegate.decrypt(encryptedStr, key, clazz).doOnSuccess(new Consumer<T>() {
      @Override
      public void accept(T t) throws Exception {
        OBJECT_CACHE.put(encryptedStr.hashCode(), t);
      }
    });
  }

  @Override
  public Single<String> encrypt(final Cipherable cipherable) {
    return mDelegate.encrypt(cipherable).doOnSuccess(new Consumer<String>() {
      @Override
      public void accept(String s) throws Exception {
        OBJECT_CACHE.put(s.hashCode(), cipherable);
      }
    });
  }

  @Override
  public Maybe<SecretKey> decryptKey(String encryptedKeyStr) {
    return mDelegate.decryptKey(encryptedKeyStr);
  }

  @Override
  public Maybe<String> encryptKey(SecretKey key) {
    return mDelegate.encryptKey(key);
  }
}

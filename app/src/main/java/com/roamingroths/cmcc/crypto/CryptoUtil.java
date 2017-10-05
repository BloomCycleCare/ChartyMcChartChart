package com.roamingroths.cmcc.crypto;

import android.os.AsyncTask;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.firebase.database.DatabaseError;
import com.roamingroths.cmcc.utils.Callbacks;
import com.roamingroths.cmcc.utils.GsonUtil;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;

/**
 * Created by parkeroth on 5/1/17.
 */

public class CryptoUtil {

  private static boolean DEBUG = false;
  private static String TAG = CryptoUtil.class.getSimpleName();

  private static final String PERSONAL_PRIVATE_KEY_ALIAS = "PersonalPrivateKey";
  private static final String KEY_STORE = "AndroidKeyStore";
  private static final Executor EXECUTOR = Executors.newFixedThreadPool(4);
  private static PublicKey PUBLIC_KEY = null;
  private static PrivateKey PRIVATE_KEY = null;

  private static final Cache<Integer, Object> OBJECT_CACHE =
      CacheBuilder.newBuilder().maximumSize(100).build();

  public static boolean initFromKeyStore() {
    try {
      KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
      ks.load(null);
      if (!ks.containsAlias(PERSONAL_PRIVATE_KEY_ALIAS)) {
        return false;
      }
      KeyStore.PrivateKeyEntry entry =
          (KeyStore.PrivateKeyEntry) ks.getEntry(PERSONAL_PRIVATE_KEY_ALIAS, null);
      if (DEBUG) Log.v(TAG, "Found key in KeyStore");
      init(new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey()));
      return true;
    } catch (Exception e) {
      Log.w("CryptoUtil", e.getMessage());
      return false;
    }
  }

  public static void init() throws CyrptoExceptions.CryptoException {
    try {
      if (DEBUG) Log.v(TAG, "Creating new KeyPair");
      KeyPair keyPair = RsaCryptoUtil.createKeyPair();
      populateKeystore(keyPair);
      init(keyPair);
    } catch (Exception e) {
      throw new CyrptoExceptions.CryptoException(e);
    }
  }

  private static void populateKeystore(KeyPair keyPair) throws CyrptoExceptions.CryptoException {
    try {
      if (DEBUG) Log.v(TAG, "Adding keys to KeyStore");
      KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
      ks.load(null);
      Certificate cert = RsaCryptoUtil.createCertificate(keyPair);
      ks.setKeyEntry(
          PERSONAL_PRIVATE_KEY_ALIAS, keyPair.getPrivate(), null, new Certificate[]{cert});
    } catch (Exception e) {
      throw new CyrptoExceptions.CryptoException(e);
    }
  }

  public static void init(String publicKeyStr, String privateKeyStr, String pbePassword)
      throws CyrptoExceptions.CryptoException {
    try {
      PublicKey publicKey = RsaCryptoUtil.parsePublicKey(publicKeyStr);
      PrivateKey privateKey = PbeCryptoUtil.unwrapPrivateKey(pbePassword, privateKeyStr);
      if (DEBUG) Log.v(TAG, "Keys parsed successfully");
      KeyPair keyPair = new KeyPair(publicKey, privateKey);
      populateKeystore(keyPair);
      init(keyPair);
    } catch (Exception e) {
      throw new CyrptoExceptions.CryptoException(e);
    }
  }

  private static void init(KeyPair keyPair) {
    PUBLIC_KEY = keyPair.getPublic();
    PRIVATE_KEY = keyPair.getPrivate();
  }

  public static String getPublicKeyStr() throws CyrptoExceptions.CryptoException {
    try {
      return RsaCryptoUtil.serializePublicKey(PUBLIC_KEY);
    } catch (Exception e) {
      throw new CyrptoExceptions.CryptoException(e);
    }
  }

  public static String getWrappedPrivateKeyStr(String password) throws CyrptoExceptions.CryptoException {
    try {
      return PbeCryptoUtil.wrapPrivateKey(password, PRIVATE_KEY);
    } catch (Exception e) {
      throw new CyrptoExceptions.CryptoException(e);
    }
  }

  public static SecretKey createSecretKey() {
    try {
      return AesCryptoUtil.createKey();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public static void encryptKey(SecretKey key, Callbacks.Callback<String> callback) {
    encrypt(AesCryptoUtil.serializeKey(key), callback);
  }

  public static void encrypt(String initialText, final Callbacks.Callback<String> callback) {
    new AsyncTask<String, Integer, String>() {
      @Override
      protected String doInBackground(String... params) {
        String rawText = params[0];
        try {
          callback.acceptData(RsaCryptoUtil.encrypt(PUBLIC_KEY, rawText));
        } catch (Exception e) {
          callback.handleError(DatabaseError.fromException(e));
        }
        return null;
      }
    }.executeOnExecutor(EXECUTOR, initialText);
  }

  public static void encrypt(final Cipherable cipherable, final Callbacks.Callback<String> callback) {
    if (DEBUG) Log.v(TAG, "Encrypting " + cipherable.getClass().getName());
    encrypt(GsonUtil.getGsonInstance().toJson(cipherable), cipherable.getKey(),
        new Callbacks.ErrorForwardingCallback<String>(callback) {
          @Override
          public void acceptData(String encryptedText) {
            callback.acceptData(encryptedText);
            OBJECT_CACHE.put(encryptedText.hashCode(), cipherable);
          }
        });
  }

  public static void encrypt(final String initialText, final SecretKey key, final Callbacks.Callback<String> callback) {
    new AsyncTask<String, Integer, String>() {
      @Override
      protected String doInBackground(String... params) {
        try {
          callback.acceptData(AesCryptoUtil.encrypt(key, initialText));
        } catch (Exception e) {
          Log.w("CryptoUtil", e.getMessage());
          callback.handleError(DatabaseError.fromException(e));
        }
        return null;
      }
    }.executeOnExecutor(EXECUTOR, initialText);
  }

  public static <T> void decrypt(final String encryptedText, SecretKey key, final Class<T> clazz, Callbacks.Callback<T> callback) {
    Object cachedObject = OBJECT_CACHE.getIfPresent(encryptedText.hashCode());
    if (cachedObject != null) {
      if (DEBUG) Log.v(TAG, "Served " + clazz.getName() + " from local cache");
      callback.acceptData((T) cachedObject);
      return;
    }
    Function<String, T> transformer = new Function<String, T>() {
      @Override
      public T apply(String decryptedStr) {
        if (DEBUG) Log.v(TAG, "Decrypting " + clazz.getName());
        T decryptedObject = Preconditions.checkNotNull(
            GsonUtil.getGsonInstance().fromJson(decryptedStr, clazz));
        OBJECT_CACHE.put(encryptedText.hashCode(), decryptedObject);
        return decryptedObject;
      }
    };
    decrypt(encryptedText, key, new Callbacks.TransformingCallback<>(callback, transformer));
  }

  public static void decryptKey(String encryptedKey, Callbacks.Callback<SecretKey> callback) {
    Function<String, SecretKey> transformer = new Function<String, SecretKey>() {
      @Override
      public SecretKey apply(String decryptedStr) {
        return AesCryptoUtil.parseKey(decryptedStr);
      }
    };
    decrypt(encryptedKey, new Callbacks.TransformingCallback<>(callback, transformer));
  }

  public static void decrypt(final String encryptedText, final SecretKey key, final Callbacks.Callback<String> callback) {
    new AsyncTask<String, Integer, String>() {
      @Override
      protected String doInBackground(String... params) {
        if (DEBUG) Log.v(TAG, "Begin symetric decryption");
        try {
          String decrypted = AesCryptoUtil.decrypt(key, encryptedText);
          if (Strings.isNullOrEmpty(decrypted)) {
            callback.handleNotFound();
          } else {
            callback.acceptData(decrypted);
          }
        } catch (Exception e) {
          Log.w("CryptoUtil", "Exception: " + e.getMessage());
          callback.handleError(DatabaseError.fromException(e));
        }
        return null;
      }
    }.executeOnExecutor(EXECUTOR, encryptedText);
  }

  public static void decrypt(String encryptedText, final Callbacks.Callback<String> callback) {
    new AsyncTask<String, Integer, String>() {
      @Override
      protected String doInBackground(String... params) {
        try {
          if (DEBUG) Log.v(TAG, "Begin decryption");
          String encryptedText = params[0];
          String decryptedText = RsaCryptoUtil.decrypt(PRIVATE_KEY, encryptedText);
          if (Strings.isNullOrEmpty(decryptedText)) {
            callback.handleNotFound();
          } else {
            callback.acceptData(decryptedText);
          }
        } catch (Exception e) {
          callback.handleError(DatabaseError.fromException(e));
        }
        return null;
      }
    }.executeOnExecutor(EXECUTOR, encryptedText);
  }

}

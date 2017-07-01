package com.roamingroths.cmcc.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.firebase.database.DatabaseError;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.security.auth.x500.X500Principal;

/**
 * Created by parkeroth on 5/1/17.
 */

public class CryptoUtil {

  private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";
  private static final String PERSONAL_PRIVATE_KEY_ALIAS = "PersonalPrivateKey";
  private static final String KEY_STORE = "AndroidKeyStore";
  private static final Executor EXECUTOR = Executors.newFixedThreadPool(4);
  private static PublicKey PUBLIC_KEY = null;
  private static PrivateKey PRIVATE_KEY = null;

  public static void init(Context context) throws CryptoException {
    if (PUBLIC_KEY == null) {
      PUBLIC_KEY = getPersonalPublicKey(context);
    }
    if (PRIVATE_KEY == null) {
      PRIVATE_KEY = getPersonalPrivateKey(context);
    }
  }

  private static final Cache<Integer, Object> OBJECT_CACHE =
      CacheBuilder.newBuilder().maximumSize(100).build();

  private static void createPersonalPrivateKey(Context context) throws CryptoException {
    try {
      Calendar start = Calendar.getInstance();
      Calendar end = Calendar.getInstance();
      end.add(Calendar.YEAR, 100);
      KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
          .setAlias(PERSONAL_PRIVATE_KEY_ALIAS)
          .setSubject(new X500Principal("CN=ChartEntries, O=AndroidAuthority"))
          .setSerialNumber(BigInteger.ONE)
          .setStartDate(start.getTime())
          .setEndDate(end.getTime())
          .build();
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", KEY_STORE);
      generator.initialize(spec);
      generator.generateKeyPair();
    } catch (Exception e) {
      throw new CryptoException(e);
    }
  }

  private static KeyStore.PrivateKeyEntry getPersonalPrivateKeyEntry(Context context) throws CryptoException {
    try {
      KeyStore keyStore = KeyStore.getInstance(KEY_STORE);
      keyStore.load(null);
      if (!keyStore.containsAlias(PERSONAL_PRIVATE_KEY_ALIAS)) {
        createPersonalPrivateKey(context);
      }
      return (KeyStore.PrivateKeyEntry) keyStore.getEntry(PERSONAL_PRIVATE_KEY_ALIAS, null);
    } catch (Exception e) {
      throw new CryptoException(e);
    }
  }

  private static PublicKey getPersonalPublicKey(Context context) throws CryptoException {
    return getPersonalPrivateKeyEntry(context).getCertificate().getPublicKey();
  }

  private static PrivateKey getPersonalPrivateKey(Context context) throws CryptoException {
    return getPersonalPrivateKeyEntry(context).getPrivateKey();
  }

  public static void encrypt(final Object object, Context context, final Callbacks.Callback<String> callback) {
    Log.v("CryptoUtil", "Encrypting " + object.getClass().getName());
    encrypt(GsonUtil.getGsonInstance().toJson(object), context,
        new Callbacks.ErrorForwardingCallback<String>(callback) {
          @Override
          public void acceptData(String encryptedText) {
            callback.acceptData(encryptedText);
            OBJECT_CACHE.put(encryptedText.hashCode(), object);
          }
        });
  }

  public static void encrypt(String initialText, final Context context, final Callbacks.Callback<String> callback) {
    new AsyncTask<String, Integer, String>() {
      @Override
      protected String doInBackground(String... params) {
        String rawText = params[0];
        try {
          Cipher input = Cipher.getInstance(TRANSFORMATION);
          if (PUBLIC_KEY == null) {
            init(context);
          }
          input.init(Cipher.ENCRYPT_MODE, PUBLIC_KEY);

          ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
          CipherOutputStream cipherOutputStream = new CipherOutputStream(
              outputStream, input);
          cipherOutputStream.write(rawText.getBytes("UTF-8"));
          cipherOutputStream.close();

          byte[] vals = outputStream.toByteArray();
          return new String(Base64.encodeToString(vals, Base64.DEFAULT));
        } catch (Exception e) {
          callback.handleError(DatabaseError.fromException(e));
        }
        return null;
      }

      @Override
      protected void onPostExecute(String s) {
        super.onPostExecute(s);
        callback.acceptData(s);
      }
    }.executeOnExecutor(EXECUTOR, initialText);
  }

  public static <T> void decrypt(
      final String encryptedText, Context context, final Class<T> clazz, Callbacks.Callback<T> callback) {
    Object cachedObject = OBJECT_CACHE.getIfPresent(encryptedText.hashCode());
    if (cachedObject != null) {
      Log.v("CryptoUtil", "Served " + clazz.getName() + " from local cache");
      callback.acceptData((T) cachedObject);
      return;
    }
    Function<String, T> transformer = new Function<String, T>() {
      @Override
      public T apply(String decryptedStr) {
        Log.v("CryptoUtil", "Decrypting " + clazz.getName());
        T decryptedObject = Preconditions.checkNotNull(
            GsonUtil.getGsonInstance().fromJson(decryptedStr, clazz));
        OBJECT_CACHE.put(encryptedText.hashCode(), decryptedObject);
        return decryptedObject;
      }
    };
    decrypt(encryptedText, context, new Callbacks.TransformingCallback<String, T>(callback, transformer));
  }

  public static void decrypt(String encryptedText, final Context context, final Callbacks.Callback<String> callback) {
    new AsyncTask<String, Integer, String>() {
      @Override
      protected String doInBackground(String... params) {
        try {
          Log.v("CryptoUtil", "Begin decryption");
          String encryptedText = params[0];
          Cipher output = Cipher.getInstance(TRANSFORMATION);
          if (PRIVATE_KEY == null) {
            init(context);
          }
          output.init(Cipher.DECRYPT_MODE, PRIVATE_KEY);

          String cipherText = encryptedText;
          CipherInputStream cipherInputStream = new CipherInputStream(
              new ByteArrayInputStream(Base64.decode(cipherText, Base64.DEFAULT)), output);
          ArrayList<Byte> values = new ArrayList<>();
          int nextByte;
          while ((nextByte = cipherInputStream.read()) != -1) {
            values.add((byte) nextByte);
          }

          byte[] bytes = new byte[values.size()];
          for (int i = 0; i < bytes.length; i++) {
            bytes[i] = values.get(i).byteValue();
          }

          String outText = new String(bytes, 0, bytes.length, "UTF-8");
          Log.v("CryptoUtil", "Finish decryption");
          return outText;
        } catch (Exception e) {
          callback.handleError(DatabaseError.fromException(e));
        }
        return null;
      }

      @Override
      protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (Strings.isNullOrEmpty(s)) {
          callback.handleNotFound();
        } else {
          callback.acceptData(s);
        }
      }
    }.executeOnExecutor(EXECUTOR, encryptedText);
  }

  public static class CryptoException extends Exception {
    public CryptoException(Exception e) {
      super(e);
    }
  }

}

package com.bloomcyclecare.cmcc.crypto;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;

/**
 * Created by parkeroth on 8/27/17.
 */

public class AesCryptoUtil {

  private static final int KEY_SIZE = 128;
  private static final String ALG = "AES/CBC/PKCS5Padding";
  private static final Joiner ON_BAR = Joiner.on("|");

  public static SecretKey createKey() throws NoSuchAlgorithmException {
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(KEY_SIZE);
    return keyGen.generateKey();
  }

  @Deprecated
  public static String encrypt(SecretKey key, String rawText) throws Exception {
    return encryptRx(key, rawText).blockingGet();
  }

  public static Single<String> encryptRx(final SecretKey key, final String decryptedStr) {
    return Single.create(new SingleOnSubscribe<String>() {
      @Override
      public void subscribe(SingleEmitter<String> e) throws Exception {
        Cipher cipher = Cipher.getInstance(ALG);
        IvParameterSpec iv = new IvParameterSpec(createKey().getEncoded());
        String ivStr = Base64.getEncoder().encodeToString(iv.getIV());
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getEncoded(), "AES"), iv);

        byte[] cipherBytes = cipher.doFinal(decryptedStr.getBytes(Charsets.UTF_8));
        String cipherStr = Base64.getEncoder().encodeToString(cipherBytes);

        e.onSuccess(ON_BAR.join(ivStr, cipherStr));
      }
    });
  }

  public static Single<String> decryptRx(final SecretKey key, final String encryptedStr) {
    return Single.create(new SingleOnSubscribe<String>() {
      @Override
      public void subscribe(SingleEmitter<String> e) throws Exception {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(encryptedStr));
        String[] parts = encryptedStr.split("\\|");
        IvParameterSpec iv = new IvParameterSpec(Base64.getDecoder().decode(parts[0]));
        byte[] cipherBytes = Base64.getDecoder().decode(parts[1]);

        Cipher cipher = Cipher.getInstance(ALG);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getEncoded(), "AES"), iv);
        e.onSuccess(new String(cipher.doFinal(cipherBytes), Charsets.UTF_8));
      }
    });
  }

  @Deprecated
  public static String decrypt(SecretKey key, String combinedStr) throws Exception {
    return decryptRx(key, combinedStr).blockingGet();
  }

  public static String serializeKey(SecretKey key) {
    try {
      return keySerializer().apply(key);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static Function<SecretKey, String> keySerializer() {
    return new Function<SecretKey, String>() {
      @Override
      public String apply(@NonNull SecretKey key) throws Exception {
        return Base64.getEncoder().encodeToString(key.getEncoded());
      }
    };
  }

  // TODO: kill this
  public static SecretKey parseKey(String keyStr) {
    try {
      return keyParser().apply(keyStr);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static Function<String, SecretKey> keyParser() {
    return new Function<String, SecretKey>() {
      @Override
      public SecretKey apply(@NonNull String keyStr) throws Exception {
        byte[] encodedKey = Base64.getDecoder().decode(keyStr);
        return new SecretKeySpec(encodedKey, "AES");
      }
    };
  }
}

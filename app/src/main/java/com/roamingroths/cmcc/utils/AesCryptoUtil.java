package com.roamingroths.cmcc.utils;

import android.util.Base64;

import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by parkeroth on 8/27/17.
 */

public class AesCryptoUtil {

  private static final int KEY_SIZE = 128;
  private static final String ALG = "AES/ECB/PKCS5Padding"; // TODO: move to CBC

  public static SecretKey createKey() throws NoSuchAlgorithmException {
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(KEY_SIZE);
    return keyGen.generateKey();
  }

  public static String encrypt(SecretKey key, String rawText) throws Exception {
    Cipher cipher = Cipher.getInstance(ALG);
    cipher.init(Cipher.ENCRYPT_MODE, key);
    byte[] cipherBytes = cipher.doFinal(rawText.getBytes());
    return Base64.encodeToString(cipherBytes, Base64.NO_WRAP);
  }

  public static String decrypt(SecretKey key, String cipherStr) throws Exception {
    Cipher cipher = Cipher.getInstance(ALG);
    cipher.init(Cipher.DECRYPT_MODE, key);
    return new String(cipher.doFinal(Base64.decode(cipherStr, Base64.NO_WRAP)));
  }

  public static String serializeKey(SecretKey key) {
    return Base64.encodeToString(key.getEncoded(), Base64.NO_WRAP);
  }

  public static SecretKey parseKey(String keyStr) {
    byte[] encodedKey = Base64.decode(keyStr, Base64.NO_WRAP);
    return new SecretKeySpec(encodedKey, "AES");
  }
}

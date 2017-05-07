package com.roamingroths.cmcc.utils;

import android.content.Context;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;

/**
 * Created by parkeroth on 5/1/17.
 */

public class CryptoUtil {

  private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";
  private static final String PERSONAL_PRIVATE_KEY_ALIAS = "PersonalPrivateKey";
  private static final String KEY_STORE = "AndroidKeyStore";

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

  public static PublicKey getPersonalPublicKey(Context context) throws CryptoException {
    return getPersonalPrivateKeyEntry(context).getCertificate().getPublicKey();
  }

  public static PrivateKey getPersonalPrivateKey(Context context) throws CryptoException {
    return getPersonalPrivateKeyEntry(context).getPrivateKey();
  }

  public static String encrypt(Object object, PublicKey publicKey) throws CryptoException {
    return encrypt(new Gson().toJson(object), publicKey);
  }

  public static String encrypt(String initialText, PublicKey publicKey) throws CryptoException {
    try {
      Cipher input = Cipher.getInstance(TRANSFORMATION);
      input.init(Cipher.ENCRYPT_MODE, publicKey);

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      CipherOutputStream cipherOutputStream = new CipherOutputStream(
          outputStream, input);
      cipherOutputStream.write(initialText.getBytes("UTF-8"));
      cipherOutputStream.close();

      byte[] vals = outputStream.toByteArray();
      return new String(Base64.encodeToString(vals, Base64.DEFAULT));
    } catch (Exception e) {
      throw new CryptoException(e);
    }
  }

  public static <T> T decrypt(String encryptedText, PrivateKey privateKey, Class<T> clazz)
      throws CryptoException {
    String json = decrypt(encryptedText, privateKey);
    return new Gson().fromJson(json, clazz);
  }

  public static String decrypt(String encryptedText, PrivateKey privateKey) throws CryptoException {
    try {
      Cipher output = Cipher.getInstance(TRANSFORMATION);
      output.init(Cipher.DECRYPT_MODE, privateKey);

      String cipherText = encryptedText;
      CipherInputStream cipherInputStream = new CipherInputStream(
          new ByteArrayInputStream(Base64.decode(cipherText, Base64.DEFAULT)), output);
      ArrayList<Byte> values = new ArrayList<>();
      int nextByte;
      while ((nextByte = cipherInputStream.read()) != -1) {
        values.add((byte)nextByte);
      }

      byte[] bytes = new byte[values.size()];
      for(int i = 0; i < bytes.length; i++) {
        bytes[i] = values.get(i).byteValue();
      }

      return new String(bytes, 0, bytes.length, "UTF-8");
    } catch (Exception e) {
      throw new CryptoException(e);
    }
  }

  public static class CryptoException extends Exception {
    public CryptoException(Exception e) {
      super(e);
    }
  }
}

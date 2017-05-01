package com.roamingroths.cmcc.utils;

import com.google.gson.Gson;

import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

/**
 * Created by parkeroth on 5/1/17.
 */

public class CryptoUtil {

  private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";

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
      return Base64.encodeBase64String(vals);
    } catch (Exception e) {
      throw new CryptoException(e);
    }
  }

  public static <T> T decrypt(String encryptedText, PrivateKey privateKey, Class<T> clazz)
      throws CryptoException {
    return new Gson().fromJson(decrypt(encryptedText, privateKey), clazz);
  }

  public static String decrypt(String encryptedText, PrivateKey privateKey) throws CryptoException {
    try {
      Cipher output = Cipher.getInstance(TRANSFORMATION);
      output.init(Cipher.DECRYPT_MODE, privateKey);

      String cipherText = encryptedText;
      CipherInputStream cipherInputStream = new CipherInputStream(
          new ByteArrayInputStream(Base64.decodeBase64(cipherText)), output);
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

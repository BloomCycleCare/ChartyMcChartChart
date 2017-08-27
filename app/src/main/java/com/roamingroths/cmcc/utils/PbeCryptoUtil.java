package com.roamingroths.cmcc.utils;

import android.util.Base64;

import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 * Created by parkeroth on 8/27/17.
 */

public class PbeCryptoUtil {

  private static final int SALT_SIZE = 8;
  private static final int HASH_ITERATIONS = 20;
  private static final int KEY_LENGTH = 64;
  private static final String ALG = "PBEWITHSHAAND128BITAES-CBC-BC";

  private static SecretKey getKey(String password, PBEParameterSpec parameterSpec)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    SecretKeyFactory keyFac = SecretKeyFactory.getInstance(ALG);
    PBEKeySpec pbeKeySpec = new PBEKeySpec(
        password.toCharArray(), parameterSpec.getSalt(), parameterSpec.getIterationCount(), KEY_LENGTH);
    return keyFac.generateSecret(pbeKeySpec);
  }

  private static PBEParameterSpec getPbeParamSpec() {
    SecureRandom random = new SecureRandom();
    byte[] salt = new byte[SALT_SIZE];
    random.nextBytes(salt);
    return new PBEParameterSpec(salt, HASH_ITERATIONS);
  }

  private static Cipher getCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
    return Cipher.getInstance(ALG);
  }

  private static AlgorithmParameters getAlgParams(PBEParameterSpec pbeParamSpec)
      throws NoSuchAlgorithmException, InvalidParameterSpecException {
    AlgorithmParameters algParams = AlgorithmParameters.getInstance(ALG);
    algParams.init(pbeParamSpec);
    return algParams;
  }

  public static String wrapPrivateKey(String password, PrivateKey privateKey) throws Exception {
    byte[] encodedPrivateKey = privateKey.getEncoded();

    PBEParameterSpec params = getPbeParamSpec();
    SecretKey pbeKey = getKey(password, params);

    // Wrap RSA private key with PBE
    Cipher cipher = getCipher();
    cipher.init(Cipher.ENCRYPT_MODE, pbeKey, params);
    byte[] cipherBytes = cipher.doFinal(encodedPrivateKey);

    AlgorithmParameters algParams = getAlgParams(params);
    EncryptedPrivateKeyInfo encInfo = new EncryptedPrivateKeyInfo(algParams, cipherBytes);

    return Base64.encodeToString(encInfo.getEncoded(), Base64.NO_WRAP);
  }

  public static PrivateKey unwrapPrivateKey(String password, String cipherText) throws Exception {
    byte[] infoBytes = Base64.decode(cipherText, Base64.NO_WRAP);
    EncryptedPrivateKeyInfo encInfo = new EncryptedPrivateKeyInfo(infoBytes);
    PBEParameterSpec parameterSpec =
        encInfo.getAlgParameters().getParameterSpec(PBEParameterSpec.class);

    SecretKey pbeKey = getKey(password, parameterSpec);

    PKCS8EncodedKeySpec pvkKeySpec = encInfo.getKeySpec(pbeKey);

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    return keyFactory.generatePrivate(pvkKeySpec);
  }
}

package com.bloomcyclecare.cmcc.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

/**
 * Created by parkeroth on 11/4/17.
 */

public class KeyUtil {

  public static KeyPair createKeyPair() throws NoSuchAlgorithmException {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(2048);
    return kpg.genKeyPair();
  }

  public static Certificate createCertificate(KeyPair keyPair) throws Exception {
    return RsaCryptoUtil.createCertificate(keyPair);
  }

  public static KeyPair parseKeyPair(String publicKeyStr, String privateKeyStr, String privateKeyPassword) throws Exception {
    PublicKey publicKey = RsaCryptoUtil.parsePublicKey(publicKeyStr);
    PrivateKey privatKey = PbeCryptoUtil.unwrapPrivateKey(privateKeyPassword, privateKeyStr);
    return new KeyPair(publicKey, privatKey);
  }

  public static String serializePublicKey(PublicKey publicKey) throws Exception {
    return RsaCryptoUtil.serializePublicKey(publicKey).call();
  }

  public static String wrapKey(PrivateKey privateKey, String password) throws Exception {
    return PbeCryptoUtil.wrapPrivateKey(password, privateKey);
  }
}
